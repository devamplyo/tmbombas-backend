package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.config.NfeProperties;
import com.projeto.th_piscinas_api.config.NfseProperties;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceResponse;
import com.projeto.th_piscinas_api.dto.invoice.NfeEmitRequest;
import com.projeto.th_piscinas_api.model.Address;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.Sale;
import com.projeto.th_piscinas_api.model.SaleItem;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.repository.InvoiceRepository;
import com.projeto.th_piscinas_api.repository.SaleRepository;
import com.projeto.th_piscinas_api.util.InvoiceType;
import com.projeto.th_piscinas_api.util.NfseStatus;
import com.projeto.th_piscinas_api.util.PaymentMethod;
import com.projeto.th_piscinas_api.util.SaleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NF-e (product invoice, model 55) issued from a sale.
 *
 * <p>With {@code nfse.simulate=true} nothing is sent to Focus: the note stays "processing" and is
 * authorized on the next lookup (after ~3 s), marked "NO FISCAL VALUE". With {@code simulate=false}
 * it goes to Focus ({@code /v2/nfe}). Token, environment and the production lock are the same as
 * the NFS-e ({@link NfseProperties}).</p>
 *
 * <p>Value, items and payment come from the sale in the database, never from the front. The payload
 * follows the one authorized by SEFAZ in homologation (09/20/2026, Simples Nacional issuer).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NfeService {

    /** "Live" statuses: block a second NF-e for the same sale. */
    static final List<NfseStatus> ATIVOS = List.of(NfseStatus.PROCESSANDO, NfseStatus.AUTORIZADO);

    private static final int JUSTIFICATIVA_MIN = 15;
    private static final int JUSTIFICATIVA_MAX = 255;
    private static final ZoneOffset OFFSET = ZoneOffset.of("-03:00");
    /** SEFAZ requires this recipient name on notes issued in homologation. */
    static final String NOME_HOMOLOGACAO = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";

    private final InvoiceRepository invoiceRepository;
    private final SaleRepository saleRepository;
    private final ClientRepository clientRepository;
    private final NfseProperties nfse;
    private final NfeProperties nfe;
    private final FocusNfeClient focus;
    private final InvoiceViewMapper view;

    /** Who receives the invoice, already resolved (registered client or data typed at issuance). */
    record Recipient(String name, String document, String stateRegistration,
                     String street, String number, String district, String city, String state, String zip) {
        boolean isCpf() { return document.length() == 11; }
        boolean isento() { return "ISENTO".equals(stateRegistration); }
        boolean contribuinte() { return !stateRegistration.isBlank() && !isento(); }
    }

    // ---------- issuance ----------

    @Transactional
    public InvoiceResponse emit(NfeEmitRequest req) {
        log.info("Emitindo NF-e - saleId={} clientId={} simulate={}", req.saleId(), req.clientId(), nfse.isSimulate());

        Sale sale = saleRepository.findById(req.saleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda não encontrada."));
        if (sale.getStatus() != SaleStatus.ATIVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível emitir NF-e de venda cancelada.");
        }
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A venda não tem itens.");
        }
        if (invoiceRepository.existsBySaleIdAndDocumentTypeAndStatusIn(sale.getId(), InvoiceType.NFE, ATIVOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma NF-e para esta venda.");
        }

        Recipient recipient = resolveRecipient(req, sale);
        validateRecipient(recipient);
        ensurePodeEmitir();

        Invoice invoice = Invoice.builder()
                .documentType(InvoiceType.NFE)
                .reference("sale-" + sale.getId() + "-" + System.currentTimeMillis())
                .saleId(sale.getId())
                .clientName(recipient.name())
                .clientDocument(recipient.document())
                .clientAddress(formatAddress(recipient))
                .description("Venda #" + sale.getId())
                .value(sale.getTotal())
                .status(NfseStatus.PROCESSANDO)
                .build();

        if (!nfse.isSimulate()) {
            emitReal(invoice, sale, recipient);
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("NF-e enviada - id={} status={} reference={}", saved.getId(), saved.getStatus(), saved.getReference());
        return view.toResponse(saved);
    }

    private void emitReal(Invoice invoice, Sale sale, Recipient recipient) {
        try {
            Map<String, Object> payload = buildPayload(sale, recipient);
            FocusNfeClient.FocusResult result = focus.emitNfe(invoice.getReference(), payload);
            applyFocusResult(invoice, result.body());
            if (result.httpStatus() >= 400 && invoice.getStatus() == NfseStatus.PROCESSANDO) {
                invoice.setStatus(NfseStatus.ERRO);
                if (invoice.getErrorMessage() == null) {
                    invoice.setErrorMessage("A Focus recusou o envio (HTTP " + result.httpStatus() + ").");
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao emitir NF-e {} na Focus: {}", invoice.getReference(), e.getMessage());
            invoice.setStatus(NfseStatus.ERRO);
            invoice.setErrorMessage("Não foi possível falar com a Focus NFe: " + e.getMessage());
        }
    }

    /**
     * Builds the NF-e payload (Simples Nacional issuer). Package-private for tests.
     * Each product uses its own fiscal data; while it has none, the company defaults apply
     * (only meant for testing — the accountant must confirm the real ones).
     */
    Map<String, Object> buildPayload(Sale sale, Recipient r) {
        boolean homologacao = !"producao".equalsIgnoreCase(nfse.getAmbiente());
        boolean interna = r.state().equalsIgnoreCase(nfe.getUfEmitente());

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("natureza_operacao", nfe.getNaturezaOperacao());
        p.put("data_emissao", OffsetDateTime.now(OFFSET).withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        p.put("tipo_documento", 1);
        p.put("local_destino", interna ? 1 : 2);
        p.put("finalidade_emissao", 1);
        p.put("consumidor_final", r.contribuinte() ? 0 : 1);
        p.put("presenca_comprador", 1);
        p.put("modalidade_frete", 9);
        p.put("cnpj_emitente", FocusSupport.digits(nfse.getCnpjPrestador()));
        p.put("inscricao_estadual_emitente", FocusSupport.digits(nfe.getIeEmitente()));
        p.put("regime_tributario_emitente", nfe.getRegimeTributario());

        p.put(r.isCpf() ? "cpf_destinatario" : "cnpj_destinatario", r.document());
        p.put("nome_destinatario", homologacao ? NOME_HOMOLOGACAO : r.name());
        if (r.contribuinte()) {
            p.put("inscricao_estadual_destinatario", r.stateRegistration());
            p.put("indicador_inscricao_estadual_destinatario", 1);
        } else {
            p.put("indicador_inscricao_estadual_destinatario", r.isento() ? 2 : 9);
        }
        p.put("logradouro_destinatario", r.street());
        p.put("numero_destinatario", r.number());
        p.put("bairro_destinatario", r.district());
        p.put("municipio_destinatario", r.city());
        p.put("uf_destinatario", r.state().toUpperCase());
        p.put("cep_destinatario", r.zip());

        List<Map<String, Object>> items = new ArrayList<>();
        int n = 1;
        for (SaleItem si : sale.getItems()) {
            items.add(buildItem(n++, si, interna));
        }
        p.put("items", items);

        BigDecimal total = sale.getItems().stream()
                .map(i -> money(i.getUnitPrice()).multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> pagamento = new LinkedHashMap<>();
        pagamento.put("forma_pagamento", paymentCode(sale.getPaymentMethod()));
        pagamento.put("valor_pagamento", total);
        p.put("formas_pagamento", List.of(pagamento));
        return p;
    }

    private Map<String, Object> buildItem(int numero, SaleItem si, boolean interna) {
        Product prod = si.getProduct();
        BigDecimal unit = money(si.getUnitPrice());
        BigDecimal bruto = unit.multiply(BigDecimal.valueOf(si.getQuantity())).setScale(2, RoundingMode.HALF_UP);
        String un = prod.getUnit() == null || prod.getUnit().isBlank() ? "UN" : prod.getUnit().trim().toUpperCase();
        if (un.length() > 6) un = un.substring(0, 6);
        String nome = prod.getName() == null ? "Produto" : prod.getName();
        if (nome.length() > 120) nome = nome.substring(0, 120);

        Map<String, Object> it = new LinkedHashMap<>();
        it.put("numero_item", numero);
        it.put("codigo_produto", prod.getCode());
        it.put("descricao", nome);
        it.put("codigo_ncm", firstNonBlank(prod.getNcm(), nfe.getNcmPadrao()));
        it.put("cfop", firstNonBlank(prod.getCfop(), interna ? nfe.getCfopPadrao() : nfe.getCfopInterestadualPadrao()));
        it.put("unidade_comercial", un);
        it.put("quantidade_comercial", si.getQuantity());
        it.put("valor_unitario_comercial", unit);
        it.put("valor_bruto", bruto);
        it.put("unidade_tributavel", un);
        it.put("quantidade_tributavel", si.getQuantity());
        it.put("valor_unitario_tributavel", unit);
        it.put("inclui_no_total", 1);
        it.put("icms_origem", prod.getOrigin() != null ? prod.getOrigin() : nfe.getOrigemPadrao());
        it.put("icms_situacao_tributaria", firstNonBlank(prod.getCsosn(), nfe.getCsosnPadrao()));
        // Simples Nacional: PIS/COFINS "other output operations", with zero base and rate.
        it.put("pis_situacao_tributaria", nfe.getPisCofinsCstPadrao());
        it.put("pis_base_calculo", 0);
        it.put("pis_aliquota_porcentual", 0);
        it.put("pis_valor", 0);
        it.put("cofins_situacao_tributaria", nfe.getPisCofinsCstPadrao());
        it.put("cofins_base_calculo", 0);
        it.put("cofins_aliquota_porcentual", 0);
        it.put("cofins_valor", 0);
        return it;
    }

    /** Payment method → Focus code (tPag). */
    static String paymentCode(PaymentMethod m) {
        if (m == null) return "99";
        return switch (m) {
            case DINHEIRO -> "01";
            case CARTAO_CREDITO -> "03";
            case CARTAO_DEBITO -> "04";
            case BOLETO -> "15";
            case PIX -> "17";
            case TRANSFERENCIA -> "18";
        };
    }

    // ---------- recipient ----------

    private Recipient resolveRecipient(NfeEmitRequest req, Sale sale) {
        if (req.clientId() != null) {
            Client c = clientRepository.findById(req.clientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
            Address a = c.getAddress() == null ? new Address() : c.getAddress();
            return new Recipient(trim(c.getName()), FocusSupport.digits(c.getDocument()), cleanIe(c.getStateRegistration()),
                    trim(a.getStreet()), trim(a.getNumber()), trim(a.getDistrict()), trim(a.getCity()),
                    trim(a.getState()), FocusSupport.digits(a.getZipCode()));
        }
        String name = firstNonBlank(req.name(), sale.getCustomerName(), "Consumidor");
        return new Recipient(name.trim(), FocusSupport.digits(req.document()), cleanIe(req.stateRegistration()),
                trim(req.street()), trim(req.number()), trim(req.district()), trim(req.city()),
                trim(req.state()), FocusSupport.digits(req.zipCode()));
    }

    /** Refuses here, in plain Portuguese, what SEFAZ would refuse later (invalid document, missing address). */
    void validateRecipient(Recipient r) {
        if (r.document().length() == 11) {
            if (!FocusSupport.isValidCpf(r.document())) {
                throw bad("CPF inválido. Confira os números.");
            }
        } else if (r.document().length() == 14) {
            if (!FocusSupport.isValidCnpj(r.document())) {
                throw bad("CNPJ inválido. Confira os números.");
            }
        } else {
            throw bad("Informe o CPF (11 números) ou o CNPJ (14 números) do cliente.");
        }
        if (r.name().isBlank()) {
            throw bad("Informe o nome do cliente.");
        }
        List<String> faltando = new ArrayList<>();
        if (r.street().isBlank()) faltando.add("rua");
        if (r.number().isBlank()) faltando.add("número");
        if (r.district().isBlank()) faltando.add("bairro");
        if (r.city().isBlank()) faltando.add("cidade");
        if (r.state().length() != 2) faltando.add("UF (2 letras)");
        if (r.zip().length() != 8) faltando.add("CEP (8 números)");
        if (!faltando.isEmpty()) {
            throw bad("Falta o endereço do cliente: " + String.join(", ", faltando)
                    + ". Complete o cadastro do cliente ou informe na emissão.");
        }
    }

    // ---------- lookup ----------

    @Transactional
    public InvoiceResponse consult(Long id) {
        Invoice invoice = find(id);

        if (nfse.isSimulate()) {
            if (invoice.getStatus() == NfseStatus.PROCESSANDO
                    && Duration.between(invoice.getCreatedAt(), LocalDateTime.now()).toMillis() > 3000) {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                invoice.setStatus(NfseStatus.AUTORIZADO);
                invoice.setNumeroNfse(String.valueOf(rnd.nextInt(1, 9999)));
                invoice.setChaveAcesso("26" + String.format("%042d", Math.abs(rnd.nextLong()) % 1_000_000_000_000_000L));
                invoice.setProtocolo("1262600003" + String.format("%05d", rnd.nextInt(100000)));
                invoice.setUrlPdf("simulado-pdf");
                invoice.setUrlXml("simulado-xml");
                invoice.setErrorMessage(null);
                invoiceRepository.save(invoice);
                log.info("NF-e simulada autorizada - id={} numero={}", id, invoice.getNumeroNfse());
            }
            return view.toResponse(invoice);
        }

        if (invoice.getStatus() == NfseStatus.PROCESSANDO || invoice.getStatus() == NfseStatus.AUTORIZADO) {
            try {
                applyFocusResult(invoice, focus.consultNfe(invoice.getReference()).body());
                invoiceRepository.save(invoice);
            } catch (Exception e) {
                log.warn("Falha ao consultar NF-e {} na Focus: {}", invoice.getReference(), e.getMessage());
            }
        }
        return view.toResponse(invoice);
    }

    // ---------- cancellation ----------

    @Transactional
    public InvoiceResponse cancel(Long id, String justificativa) {
        Invoice invoice = find(id);

        if (invoice.getStatus() != NfseStatus.AUTORIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só é possível cancelar uma NF-e autorizada.");
        }
        String just = justificativa == null ? "" : justificativa.trim();
        if (just.length() < JUSTIFICATIVA_MIN || just.length() > JUSTIFICATIVA_MAX) {
            throw bad("O motivo do cancelamento precisa ter de " + JUSTIFICATIVA_MIN + " a " + JUSTIFICATIVA_MAX + " caracteres.");
        }

        if (nfse.isSimulate()) {
            invoice.setStatus(NfseStatus.CANCELADO);
            invoice.setCancelJustificativa(just);
            invoice.setCancelledAt(LocalDateTime.now());
            return view.toResponse(invoiceRepository.save(invoice));
        }

        ensurePodeEmitir();

        FocusNfeClient.FocusResult result;
        try {
            result = focus.cancelNfe(invoice.getReference(), just);
        } catch (Exception e) {
            log.warn("Falha ao cancelar NF-e {} na Focus: {}", invoice.getReference(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível falar com a Focus NFe para cancelar. Tente novamente.");
        }

        boolean cancelado = "cancelado".equals(String.valueOf(result.body().get("status")))
                || (result.httpStatus() < 400 && result.body().get("status_sefaz") != null);
        if (cancelado) {
            invoice.setStatus(NfseStatus.CANCELADO);
            invoice.setCancelJustificativa(just);
            invoice.setCancelledAt(LocalDateTime.now());
            invoice.setErrorMessage(null);
            return view.toResponse(invoiceRepository.save(invoice));
        }
        String erro = FocusSupport.extractError(result.body());
        throw bad(erro != null ? erro : "A SEFAZ recusou o cancelamento. O prazo pode ter passado.");
    }

    // ---------- Focus result ----------

    private void applyFocusResult(Invoice invoice, Map<String, Object> body) {
        Object status = body.get("status");
        if (status != null) {
            invoice.setStatus(FocusSupport.mapStatus(status.toString()));
        }
        if (body.get("numero") != null) {
            invoice.setNumeroNfse(body.get("numero").toString());
        }
        if (body.get("chave_nfe") != null) {
            invoice.setChaveAcesso(body.get("chave_nfe").toString().replaceFirst("^NFe", ""));
        }
        if (body.get("protocolo") != null) {
            invoice.setProtocolo(body.get("protocolo").toString());
        }
        // Focus returns RELATIVE paths ("/arquivos_development/..."); kept absolute, and only ever
        // downloaded by the server (see InvoiceService.document) so the token stays here.
        if (body.get("caminho_danfe") != null) {
            invoice.setUrlPdf(focus.absoluteFileUrl(body.get("caminho_danfe").toString()));
        }
        if (body.get("caminho_xml_nota_fiscal") != null) {
            invoice.setUrlXml(focus.absoluteFileUrl(body.get("caminho_xml_nota_fiscal").toString()));
        }
        String erro = FocusSupport.extractError(body);
        if (erro != null) {
            invoice.setErrorMessage(erro);
        } else if (invoice.getStatus() == NfseStatus.AUTORIZADO) {
            invoice.setErrorMessage(null);
        }
    }

    // ---------- configuration / diagnostics ----------

    void ensurePodeEmitir() {
        String pendencia = pendencia();
        if (pendencia != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, pendencia);
        }
    }

    /** Actionable message on what's missing to issue for real, or {@code null} if ready. */
    public String pendencia() {
        if (nfse.isSimulate()) return null;

        if (isBlank(nfse.getToken())) {
            return "Token da Focus NFe não configurado. Preencha FOCUSNFE_TOKEN no servidor e reinicie.";
        }
        if ("producao".equalsIgnoreCase(nfse.getAmbiente()) && !nfse.isProducaoConfirmada()) {
            return "Ambiente de produção não confirmado. Defina NFSE_PRODUCAO_CONFIRMADA=true para liberar emissão real.";
        }
        List<String> faltando = new ArrayList<>();
        if (isBlank(nfse.getCnpjPrestador())) faltando.add("CNPJ do emitente");
        if (isBlank(nfe.getIeEmitente())) faltando.add("inscrição estadual do emitente (NFE_IE_EMITENTE)");
        if (!faltando.isEmpty()) {
            return "Dados do emitente incompletos: " + String.join(", ", faltando) + ".";
        }
        if (nfe.getRegimeTributario() == 3) {
            return "A NF-e para Regime Normal ainda não está disponível no sistema (só Simples Nacional).";
        }
        return null;
    }

    // ---------- simulated documents (local/test mode: "NO FISCAL VALUE") ----------

    String simuladoHtml(Invoice inv) {
        StringBuilder itens = new StringBuilder();
        saleRepository.findById(inv.getSaleId() == null ? -1L : inv.getSaleId()).ifPresent(sale ->
                sale.getItems().forEach(i -> itens.append("<tr><td>").append(esc(i.getProduct().getName()))
                        .append("</td><td>").append(i.getQuantity()).append("</td><td>")
                        .append(money(i.getUnitPrice())).append("</td></tr>")));
        return "<!doctype html><html lang=\"pt-BR\"><meta charset=\"utf-8\"><title>NF-e (simulação)</title>"
                + "<body style=\"font-family:sans-serif;max-width:720px;margin:2rem auto\">"
                + "<h2>DANFE (simulação) — NF-e nº " + esc(inv.getNumeroNfse()) + "</h2>"
                + "<p style=\"color:#b00;font-weight:bold\">SEM VALOR FISCAL — documento de teste, nada foi enviado para a SEFAZ.</p>"
                + "<p><b>Chave de acesso:</b> " + esc(inv.getChaveAcesso()) + "<br><b>Protocolo:</b> " + esc(inv.getProtocolo())
                + "<br><b>Cliente:</b> " + esc(inv.getClientName()) + " (" + esc(inv.getClientDocument()) + ")"
                + "<br><b>Endereço:</b> " + esc(inv.getClientAddress()) + "</p>"
                + "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\"><tr><th>Produto</th><th>Qtd</th><th>Preço unit.</th></tr>"
                + itens + "</table><h3>Total: R$ " + money(inv.getValue()) + "</h3></body></html>";
    }

    String simuladoXml(Invoice inv) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<nfeSimulada semValorFiscal=\"true\">\n"
                + "  <numero>" + esc(inv.getNumeroNfse()) + "</numero>\n"
                + "  <chave>" + esc(inv.getChaveAcesso()) + "</chave>\n"
                + "  <destinatario>" + esc(inv.getClientName()) + "</destinatario>\n"
                + "  <valorTotal>" + money(inv.getValue()) + "</valorTotal>\n</nfeSimulada>\n";
    }

    Invoice find(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota fiscal não encontrada."));
        if (invoice.getDocumentType() != InvoiceType.NFE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta nota não é uma NF-e.");
        }
        return invoice;
    }

    // ---------- small helpers ----------

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    /** State registration: digits, or the word ISENTO; empty when none. */
    private static String cleanIe(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.trim().equalsIgnoreCase("ISENTO")) return "ISENTO";
        return FocusSupport.digits(raw);
    }

    private static String formatAddress(Recipient r) {
        return Stream.of(r.street(), r.number(), r.district(), r.city(), r.state())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
