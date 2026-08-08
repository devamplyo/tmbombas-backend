package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.config.NfseProperties;
import com.projeto.th_piscinas_api.dto.nfse.NfseEmitRequest;
import com.projeto.th_piscinas_api.dto.nfse.NfseResponse;
import com.projeto.th_piscinas_api.dto.nfse.NfseStatusResponse;
import com.projeto.th_piscinas_api.mapper.InvoiceMapper;
import com.projeto.th_piscinas_api.model.Address;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.repository.InvoiceRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.util.NfseStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nationwide NFS-e issuance from a completed Service Order.
 * With {@code simulate=true} the invoice is "NO FISCAL VALUE": it stays in
 * processing and gets authorized on the next lookup (after ~3s). With
 * {@code simulate=false} it delegates to {@link FocusNfeClient} (endpoint {@code /v2/nfsen}).
 *
 * <p>Client/value/description data comes from {@code ServiceOrder}/{@code Client}
 * in the database — not trusted from a snapshot sent by the front.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NfseService {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** "Live" statuses: block reissuance for the same service order. */
    private static final List<NfseStatus> ATIVOS = List.of(NfseStatus.PROCESSANDO, NfseStatus.AUTORIZADO);

    private static final int JUSTIFICATIVA_MIN_LENGTH = 15;

    private final InvoiceRepository invoiceRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final InvoiceMapper invoiceMapper;
    private final NfseProperties props;
    private final FocusNfeClient focusClient;
    private final DpsNumberAllocator dpsNumberAllocator;

    public NfseStatusResponse status() {
        String pendencia = pendencia();
        boolean prontoParaReal = !props.isSimulate() && pendencia == null;
        return new NfseStatusResponse(props.isConfigured(), props.getAmbiente(), props.isSimulate(),
                prontoParaReal, pendencia);
    }

    @Transactional(readOnly = true)
    public List<NfseResponse> listByServiceOrder(String serviceOrderId) {
        log.info("Listando NFS-e da OS - serviceOrderId={}", serviceOrderId);
        return invoiceRepository.findByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId)
                .stream().map(invoiceMapper::toResponse).toList();
    }

    // ---------- issuance ----------

    @Transactional
    public NfseResponse emit(NfseEmitRequest req) {
        log.info("Emitindo NFS-e - serviceOrderId={} simulate={}", req.serviceOrderId(), props.isSimulate());

        ServiceOrder order = serviceOrderRepository.findById(req.serviceOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de serviço não encontrada."));

        if (order.getStatus() != ServiceOrderStatus.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Só é possível emitir NFS-e de OS concluída.");
        }
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A OS precisa de um valor definido.");
        }

        Client client = order.getClient();
        String documento = digits(client == null ? null : client.getDocument());
        if (documento.length() != 11 && documento.length() != 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cliente sem CPF/CNPJ válido — a prefeitura recusa a nota.");
        }

        String serviceOrderId = String.valueOf(order.getId());
        if (invoiceRepository.existsByServiceOrderIdAndStatusIn(serviceOrderId, ATIVOS)) {
            log.warn("Emissão de NFS-e rejeitada - já existe nota ativa para a OS {}", serviceOrderId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma NFS-e para esta OS.");
        }

        ensurePodeEmitir();

        String descricao = (order.getDescription() == null || order.getDescription().isBlank())
                ? order.getTitle() : order.getDescription();

        Invoice invoice = Invoice.builder()
                .reference("os-" + serviceOrderId + "-" + System.currentTimeMillis())
                .serviceOrderId(serviceOrderId)
                .clientName(client.getName())
                .clientDocument(documento)
                .clientAddress(formatAddress(client))
                .description(descricao)
                .value(order.getPrice())
                .status(NfseStatus.PROCESSANDO)
                .dataCompetencia(LocalDate.now())
                .build();

        if (!props.isSimulate()) {
            String cnpj = digits(props.getCnpjPrestador());
            invoice.setSerieDps(props.getSerieDps());
            invoice.setNumeroDps(dpsNumberAllocator.next(cnpj, props.getSerieDps()));
            log.info("Delegando emissão de NFS-e à Focus - reference={} serie={} numero={}",
                    invoice.getReference(), invoice.getSerieDps(), invoice.getNumeroDps());
            emitReal(invoice);
        }

        NfseResponse response = invoiceMapper.toResponse(invoiceRepository.save(invoice));
        log.info("NFS-e emitida - id={} status={} reference={}",
                response.id(), invoice.getStatus(), invoice.getReference());
        return response;
    }

    /**
     * Builds the exact payload that would be sent to Focus for the given
     * service order, without calling Focus, without persisting anything and
     * without consuming DPS numbering. Useful to check the payload against
     * an already-validated issuance.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> preview(Long serviceOrderId) {
        ServiceOrder order = serviceOrderRepository.findById(serviceOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de serviço não encontrada."));

        Client client = order.getClient();
        Invoice fake = Invoice.builder()
                .clientName(client == null ? null : client.getName())
                .clientDocument(digits(client == null ? null : client.getDocument()))
                .description((order.getDescription() == null || order.getDescription().isBlank())
                        ? order.getTitle() : order.getDescription())
                .value(order.getPrice() == null ? BigDecimal.ZERO : order.getPrice())
                .dataCompetencia(LocalDate.now())
                .build();

        String cnpj = digits(props.getCnpjPrestador());
        long proximoNumero = dpsNumberAllocator.peekNext(cnpj, props.getSerieDps());
        return focusClient.buildPayload(fake, props.getSerieDps(), proximoNumero);
    }

    @Transactional
    public NfseResponse consult(Long id) {
        log.info("Consultando NFS-e - id={}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("NFS-e não encontrada: id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "NFS-e não encontrada.");
                });

        if (props.isSimulate()) {
            if (invoice.getStatus() == NfseStatus.PROCESSANDO
                    && Duration.between(invoice.getCreatedAt(), LocalDateTime.now()).toMillis() > 3000) {
                invoice.setStatus(NfseStatus.AUTORIZADO);
                invoice.setNumeroNfse(String.valueOf(ThreadLocalRandom.current().nextInt(10000, 99999)));
                invoice.setUrlPdf("/api/nfse/" + invoice.getId() + "/simulado?fmt=html");
                invoice.setUrlXml("/api/nfse/" + invoice.getId() + "/simulado?fmt=xml");
                invoiceRepository.save(invoice);
                log.info("NFS-e simulada autorizada - id={} numero={}", id, invoice.getNumeroNfse());
            }
            return invoiceMapper.toResponse(invoice);
        }

        if (props.isConfigured()) {
            try {
                applyFocusResult(invoice, focusClient.consult(invoice.getReference()).body());
                invoiceRepository.save(invoice);
            } catch (Exception e) {
                log.warn("Falha ao consultar NFS-e {} na Focus: {}", invoice.getReference(), e.getMessage());
            }
        }
        return invoiceMapper.toResponse(invoice);
    }

    // ---------- cancellation ----------

    @Transactional
    public NfseResponse cancel(Long id, String justificativa) {
        log.info("Cancelando NFS-e - id={}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NFS-e não encontrada."));

        if (invoice.getStatus() != NfseStatus.AUTORIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível cancelar uma NFS-e autorizada.");
        }
        if (justificativa == null || justificativa.trim().length() < JUSTIFICATIVA_MIN_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Justificativa precisa ter pelo menos " + JUSTIFICATIVA_MIN_LENGTH + " caracteres.");
        }

        if (props.isSimulate()) {
            invoice.setStatus(NfseStatus.CANCELADO);
            invoice.setCancelJustificativa(justificativa);
            invoice.setCancelledAt(LocalDateTime.now());
            return invoiceMapper.toResponse(invoiceRepository.save(invoice));
        }

        ensurePodeEmitir();

        FocusNfeClient.FocusResult result;
        try {
            result = focusClient.cancel(invoice.getReference(), justificativa);
        } catch (Exception e) {
            log.warn("Falha ao cancelar NFS-e {} na Focus: {}", invoice.getReference(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível falar com a Focus NFe para cancelar. Tente novamente.");
        }

        applyFocusResult(invoice, result.body());
        if (invoice.getStatus() == NfseStatus.CANCELADO) {
            invoice.setCancelJustificativa(justificativa);
            invoice.setCancelledAt(LocalDateTime.now());
        } else if (result.httpStatus() >= 400) {
            invoiceRepository.save(invoice);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    invoice.getErrorMessage() != null
                            ? invoice.getErrorMessage() : "A prefeitura recusou o cancelamento.");
        }

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    // ---------- real path (Focus) ----------

    private void emitReal(Invoice invoice) {
        try {
            Map<String, Object> payload = focusClient.buildPayload(invoice, invoice.getSerieDps(), invoice.getNumeroDps());
            FocusNfeClient.FocusResult result = focusClient.emit(invoice.getReference(), payload);
            applyFocusResult(invoice, result.body());
            if (result.httpStatus() >= 400 && invoice.getStatus() == NfseStatus.PROCESSANDO) {
                invoice.setStatus(NfseStatus.ERRO);
            }
        } catch (Exception e) {
            invoice.setStatus(NfseStatus.ERRO);
            invoice.setErrorMessage(e.getMessage());
        }
    }

    private void applyFocusResult(Invoice invoice, Map<String, Object> body) {
        Object status = body.get("status");
        if (status != null) {
            invoice.setStatus(mapFocusStatus(status.toString()));
        }
        if (body.get("numero") != null) {
            invoice.setNumeroNfse(body.get("numero").toString());
        }
        // "url_danfse" is the direct link to the PDF (DANFSe) on Nationwide NFS-e.
        // "url" points to the government's Public Lookup page (HTML page, not PDF)
        // - only falls back to it if Focus doesn't send url_danfse for some reason.
        if (body.get("url_danfse") != null) {
            invoice.setUrlPdf(body.get("url_danfse").toString());
        } else if (body.get("url") != null) {
            invoice.setUrlPdf(body.get("url").toString());
        }
        // "caminho_xml_nota_fiscal" comes as a RELATIVE path (e.g.: "/arquivos/...")
        // on Nationwide NFS-e, with no domain. Resolves it against the origin of
        // "url_danfse" (same file bucket) to turn it into a link that actually opens.
        if (body.get("caminho_xml_nota_fiscal") != null) {
            invoice.setUrlXml(resolveArquivoUrl(
                    body.get("caminho_xml_nota_fiscal").toString(), body.get("url_danfse")));
        }
        String erro = extractErrorMessage(body);
        if (erro != null) {
            invoice.setErrorMessage(erro);
        }
    }

    /**
     * Resolves a Focus file path against the origin of another already-absolute
     * URL from the same response (e.g.: url_danfse) when it comes in relative.
     * If it's already absolute, or there's no reference to resolve it against,
     * returns it as-is.
     */
    private static String resolveArquivoUrl(String caminho, Object urlAbsolutaReferencia) {
        if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
            return caminho;
        }
        if (urlAbsolutaReferencia != null) {
            try {
                java.net.URI referencia = java.net.URI.create(urlAbsolutaReferencia.toString());
                return referencia.getScheme() + "://" + referencia.getAuthority() + caminho;
            } catch (IllegalArgumentException ignored) {
                // falls through to the relative path below anyway
            }
        }
        return caminho;
    }

    /**
     * Extracts a readable message from Focus's two error formats: a list
     * {@code erros:[{codigo,mensagem}]} (city authorization error, e.g.
     * E0713) or {@code codigo}/{@code mensagem} at the root level (e.g. company not
     * enabled). Focus/the city's message always says which field to fix.
     */
    @SuppressWarnings("unchecked")
    private String extractErrorMessage(Map<String, Object> body) {
        Object errosObj = body.get("erros");
        if (errosObj instanceof List<?> erros && !erros.isEmpty()) {
            return erros.stream()
                    .filter(Map.class::isInstance)
                    .map(e -> (Map<String, Object>) e)
                    .map(this::formatCodigoMensagem)
                    .collect(Collectors.joining(" | "));
        }
        if (body.get("codigo") != null || body.get("mensagem") != null) {
            return formatCodigoMensagem(body);
        }
        return null;
    }

    private String formatCodigoMensagem(Map<String, Object> m) {
        Object codigo = m.get("codigo");
        Object mensagem = m.get("mensagem");
        return (codigo != null ? codigo + ": " : "") + (mensagem != null ? mensagem : "");
    }

    private NfseStatus mapFocusStatus(String focus) {
        return switch (focus) {
            case "autorizado" -> NfseStatus.AUTORIZADO;
            case "cancelado" -> NfseStatus.CANCELADO;
            case "erro_autorizacao" -> NfseStatus.ERRO_AUTORIZACAO;
            case "erro" -> NfseStatus.ERRO;
            default -> NfseStatus.PROCESSANDO; // processing, processing_authorization, ...
        };
    }

    // ---------- configuration / diagnostics ----------

    private void ensurePodeEmitir() {
        if (props.isSimulate()) return;
        String pendencia = pendencia();
        if (pendencia != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, pendencia);
        }
    }

    /** Actionable message on what's missing to issue for real, or {@code null} if ready. */
    private String pendencia() {
        if (props.isSimulate()) return null;

        if (isBlank(props.getToken())) {
            return "Token da Focus NFe não configurado. Preencha FOCUSNFE_TOKEN no .env do servidor e reinicie.";
        }
        if ("producao".equalsIgnoreCase(props.getAmbiente()) && !props.isProducaoConfirmada()) {
            return "Ambiente de produção não confirmado. Defina NFSE_PRODUCAO_CONFIRMADA=true para liberar emissão real.";
        }

        List<String> faltando = new ArrayList<>();
        if (isBlank(props.getCnpjPrestador())) faltando.add("CNPJ do prestador");
        if (isBlank(props.getCodigoMunicipio())) faltando.add("código do município");
        if (isBlank(props.getCodigoTributacaoNacional())) faltando.add("código de tributação nacional");
        if (isBlank(props.getCodigoTributacaoMunicipal())) faltando.add("código de tributação municipal");
        if (!faltando.isEmpty()) {
            return "Dados do prestador incompletos: " + String.join(", ", faltando) + ".";
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String formatAddress(Client client) {
        if (client == null || client.getAddress() == null) return "";
        Address a = client.getAddress();
        return Stream.of(a.getStreet(), a.getNumber(), a.getDistrict(), a.getCity(), a.getState())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String digits(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }

    // ---------- simulated receipt (NO FISCAL VALUE) ----------

    @Transactional(readOnly = true)
    public Invoice getInvoiceForReceipt(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NFS-e não encontrada."));
    }

    public String simuladoXml(Invoice inv) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <NFSe simulada="true">
                  <Numero>%s</Numero>
                  <Tomador>%s</Tomador>
                  <CpfCnpj>%s</CpfCnpj>
                  <Discriminacao>%s</Discriminacao>
                  <Valor>%s</Valor>
                  <DataEmissao>%s</DataEmissao>
                </NFSe>"""
                .formatted(
                        esc(inv.getNumeroNfse()),
                        esc(inv.getClientName()),
                        esc(inv.getClientDocument()),
                        esc(inv.getDescription()),
                        String.format(Locale.US, "%.2f", nz(inv.getValue())),
                        esc(inv.getCreatedAt() == null ? "" : inv.getCreatedAt().toString()));
    }

    public String simuladoHtml(Invoice inv) {
        String numero = esc(inv.getNumeroNfse() == null ? "—" : inv.getNumeroNfse());
        String emissao = inv.getCreatedAt() == null ? "—" : inv.getCreatedAt().format(DATA_HORA);
        String valor = String.format(PT_BR, "%,.2f", nz(inv.getValue()));
        String endereco = esc(inv.getClientAddress() == null || inv.getClientAddress().isBlank()
                ? "—" : inv.getClientAddress());

        return """
                <!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>NFS-e %s</title>
                <style>
                *{box-sizing:border-box}
                body{font-family:Arial,Helvetica,sans-serif;color:#111;background:#eef1f5;margin:0;padding:24px}
                .page{max-width:820px;margin:auto;background:#fff;border:1px solid #999;padding:28px;position:relative;overflow:hidden}
                .wm{position:absolute;top:45%%;left:50%%;transform:translate(-50%%,-50%%) rotate(-24deg);font-size:60px;color:rgba(200,0,0,.10);font-weight:bold;white-space:nowrap;pointer-events:none}
                h1{font-size:17px;text-align:center;margin:0 0 2px}
                .sub{text-align:center;font-size:12px;color:#555;margin-bottom:14px}
                .alert{background:#fff4e5;border:1px solid #f0a500;color:#9a5b00;text-align:center;font-size:12px;font-weight:bold;padding:7px;border-radius:4px;margin-bottom:18px}
                .box{border:1px solid #c4c4c4;border-radius:5px;padding:11px 14px;margin-bottom:11px}
                .box h2{font-size:10.5px;text-transform:uppercase;color:#777;margin:0 0 7px;letter-spacing:.05em}
                .row{display:flex;justify-content:space-between;font-size:13px;padding:3px 0;gap:16px}
                .row span:first-child{color:#777;white-space:nowrap}
                .row span:last-child{text-align:right}
                .disc{font-size:13px;line-height:1.5}
                .total{display:flex;justify-content:space-between;font-size:17px;font-weight:bold;border-top:2px solid #333;padding-top:9px;margin-top:2px}
                .actions{max-width:820px;margin:16px auto 0;text-align:right}
                .btn{background:#0891b2;color:#fff;border:none;padding:11px 20px;border-radius:6px;font-size:14px;cursor:pointer}
                @media print{body{background:#fff;padding:0}.actions{display:none}.page{border:none}}
                </style></head>
                <body>
                <div class="page">
                  <div class="wm">SEM VALOR FISCAL</div>
                  <h1>NOTA FISCAL DE SERVIÇOS ELETRÔNICA — NFS-e</h1>
                  <div class="sub">Nº %s &nbsp;·&nbsp; Emissão: %s</div>
                  <div class="alert">DOCUMENTO SIMULADO — SEM VALOR FISCAL (gerado apenas para teste)</div>

                  <div class="box">
                    <h2>Prestador de serviço</h2>
                    <div class="row"><span>Nome</span><span>%s</span></div>
                    <div class="row"><span>Serviço</span><span>Manutenção e instalação de bombas e piscinas</span></div>
                  </div>

                  <div class="box">
                    <h2>Tomador do serviço</h2>
                    <div class="row"><span>Nome</span><span>%s</span></div>
                    <div class="row"><span>CPF / CNPJ</span><span>%s</span></div>
                    <div class="row"><span>Endereço</span><span>%s</span></div>
                  </div>

                  <div class="box">
                    <h2>Discriminação dos serviços</h2>
                    <div class="disc">%s</div>
                  </div>

                  <div class="box">
                    <div class="total"><span>Valor total dos serviços</span><span>R$ %s</span></div>
                  </div>
                </div>
                <div class="actions"><button class="btn" onclick="window.print()">🖨️ Imprimir / Salvar PDF</button></div>
                </body></html>"""
                .formatted(
                        numero, numero, emissao,
                        esc(props.getRazaoSocial()),
                        esc(inv.getClientName() == null ? "—" : inv.getClientName()),
                        esc(inv.getClientDocument() == null ? "—" : inv.getClientDocument()),
                        endereco,
                        esc(inv.getDescription() == null ? "—" : inv.getDescription()),
                        valor);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
