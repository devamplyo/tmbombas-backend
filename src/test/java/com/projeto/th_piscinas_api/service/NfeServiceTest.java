package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.config.NfeProperties;
import com.projeto.th_piscinas_api.config.NfseProperties;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceResponse;
import com.projeto.th_piscinas_api.dto.invoice.NfeEmitRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * NF-e issuance. The payload mirrors the one SEFAZ authorized in homologation
 * (09/20/2026, TM Bombas, Simples Nacional): a change that breaks one of these fields
 * reproduces an error already seen there.
 */
@ExtendWith(MockitoExtension.class)
class NfeServiceTest {

    private static final String CPF_VALIDO = "11144477735";
    private static final String CNPJ_VALIDO = "65480593000186";

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private FocusNfeClient focus;

    private NfseProperties nfse;
    private NfeProperties nfe;
    private NfeService service;

    @BeforeEach
    void setUp() {
        nfse = new NfseProperties();
        nfse.setSimulate(true);
        nfse.setAmbiente("homologacao");
        nfse.setCnpjPrestador("59774374000107");
        nfe = new NfeProperties();
        nfe.setIeEmitente("123340497");
        InvoiceViewMapper view = new InvoiceViewMapper(saleRepository);
        service = new NfeService(invoiceRepository, saleRepository, clientRepository, nfse, nfe, focus, view);
        lenient().when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- builders ----------

    private Product produto(String ncm, String cfop, Integer origem, String csosn) {
        Product p = new Product();
        p.setId(1L);
        p.setName("Bomba d'agua 1CV");
        p.setCode("BOMBA-1CV");
        p.setUnit("un");
        p.setPrice(new BigDecimal("10.00"));
        p.setNcm(ncm);
        p.setCfop(cfop);
        p.setOrigin(origem);
        p.setCsosn(csosn);
        return p;
    }

    private Sale venda(SaleStatus status, PaymentMethod pagamento, Product produto) {
        Sale sale = Sale.builder()
                .id(12L)
                .customerName("João da Silva")
                .total(new BigDecimal("20.00"))
                .status(status)
                .paymentMethod(pagamento)
                .build();
        SaleItem item = SaleItem.builder().sale(sale).product(produto).quantity(2)
                .unitPrice(new BigDecimal("10.00")).subtotal(new BigDecimal("20.00")).build();
        sale.getItems().add(item);
        return sale;
    }

    private NfeService.Recipient destinatario(String doc, String ie, String uf) {
        return new NfeService.Recipient("João da Silva", doc, ie, "Rua Arquimedes de Oliveira", "205",
                "Santo Amaro", "Recife", uf, "50050510");
    }

    private NfeEmitRequest pedidoBalcao(String doc) {
        return new NfeEmitRequest(12L, null, "João da Silva", doc, null,
                "Rua Arquimedes de Oliveira", "205", "Santo Amaro", "Recife", "PE", "50050510");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> primeiroItem(Map<String, Object> payload) {
        return ((List<Map<String, Object>>) payload.get("items")).get(0);
    }

    // ---------- payload ----------

    @Test
    void buildPayload_cpfNaMesmaUf_usaOsPadroesDaEmpresa() {
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null)),
                destinatario(CPF_VALIDO, "", "PE"));

        assertThat(p).containsEntry("natureza_operacao", "Venda de mercadoria")
                .containsEntry("tipo_documento", 1)
                .containsEntry("local_destino", 1)
                .containsEntry("finalidade_emissao", 1)
                .containsEntry("consumidor_final", 1)
                .containsEntry("cnpj_emitente", "59774374000107")
                .containsEntry("inscricao_estadual_emitente", "123340497")
                .containsEntry("regime_tributario_emitente", 1)
                .containsEntry("cpf_destinatario", CPF_VALIDO)
                .containsEntry("indicador_inscricao_estadual_destinatario", 9)
                // SEFAZ requires this name on notes issued in homologation
                .containsEntry("nome_destinatario", NfeService.NOME_HOMOLOGACAO)
                .doesNotContainKey("cnpj_destinatario")
                .doesNotContainKey("inscricao_estadual_destinatario");

        Map<String, Object> item = primeiroItem(p);
        assertThat(item).containsEntry("codigo_produto", "BOMBA-1CV")
                .containsEntry("codigo_ncm", "84137090")
                .containsEntry("cfop", "5102")
                .containsEntry("unidade_comercial", "UN")
                .containsEntry("quantidade_comercial", 2)
                .containsEntry("icms_origem", 0)
                .containsEntry("icms_situacao_tributaria", "102")
                .containsEntry("pis_situacao_tributaria", "49")
                .containsEntry("cofins_situacao_tributaria", "49");
        assertThat((BigDecimal) item.get("valor_bruto")).isEqualByComparingTo("20.00");
    }

    @Test
    void buildPayload_pagamento_usaOCodigoDaFormaDePagamentoEOTotalDaVenda() {
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null)),
                destinatario(CPF_VALIDO, "", "PE"));

        @SuppressWarnings("unchecked")
        Map<String, Object> pagamento = ((List<Map<String, Object>>) p.get("formas_pagamento")).get(0);
        assertThat(pagamento).containsEntry("forma_pagamento", "17");
        assertThat((BigDecimal) pagamento.get("valor_pagamento")).isEqualByComparingTo("20.00");
    }

    @Test
    void paymentCode_mapeiaTodasAsFormas() {
        assertThat(NfeService.paymentCode(PaymentMethod.DINHEIRO)).isEqualTo("01");
        assertThat(NfeService.paymentCode(PaymentMethod.CARTAO_CREDITO)).isEqualTo("03");
        assertThat(NfeService.paymentCode(PaymentMethod.CARTAO_DEBITO)).isEqualTo("04");
        assertThat(NfeService.paymentCode(PaymentMethod.BOLETO)).isEqualTo("15");
        assertThat(NfeService.paymentCode(PaymentMethod.PIX)).isEqualTo("17");
        assertThat(NfeService.paymentCode(PaymentMethod.TRANSFERENCIA)).isEqualTo("18");
        assertThat(NfeService.paymentCode(null)).isEqualTo("99");
    }

    @Test
    void buildPayload_outraUf_usaOperacaoInterestadual() {
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null)),
                destinatario(CPF_VALIDO, "", "SP"));

        assertThat(p).containsEntry("local_destino", 2).containsEntry("uf_destinatario", "SP");
        assertThat(primeiroItem(p)).containsEntry("cfop", "6102");
    }

    @Test
    void buildPayload_dadosFiscaisDoProduto_prevalecemSobreOPadrao() {
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto("39269090", "5101", 1, "500")),
                destinatario(CPF_VALIDO, "", "PE"));

        assertThat(primeiroItem(p)).containsEntry("codigo_ncm", "39269090")
                .containsEntry("cfop", "5101")
                .containsEntry("icms_origem", 1)
                .containsEntry("icms_situacao_tributaria", "500");
    }

    @Test
    void buildPayload_empresaContribuinte_enviaInscricaoEstadualENaoEConsumidorFinal() {
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null)),
                destinatario(CNPJ_VALIDO, "123456789", "PE"));

        assertThat(p).containsEntry("cnpj_destinatario", CNPJ_VALIDO)
                .containsEntry("inscricao_estadual_destinatario", "123456789")
                .containsEntry("indicador_inscricao_estadual_destinatario", 1)
                .containsEntry("consumidor_final", 0)
                .doesNotContainKey("cpf_destinatario");
    }

    @Test
    void buildPayload_empresaIsenta_indicadorDois() {
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null)),
                destinatario(CNPJ_VALIDO, "ISENTO", "PE"));

        assertThat(p).containsEntry("indicador_inscricao_estadual_destinatario", 2)
                .doesNotContainKey("inscricao_estadual_destinatario");
    }

    @Test
    void buildPayload_producao_enviaONomeRealDoCliente() {
        nfse.setAmbiente("producao");
        Map<String, Object> p = service.buildPayload(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null)),
                destinatario(CPF_VALIDO, "", "PE"));

        assertThat(p).containsEntry("nome_destinatario", "João da Silva");
    }

    // ---------- emit ----------

    @Test
    void emit_simulado_criaNotaProcessandoSemChamarAFocus() {
        Sale sale = venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null));
        when(saleRepository.findById(12L)).thenReturn(Optional.of(sale));

        InvoiceResponse r = service.emit(pedidoBalcao(CPF_VALIDO));

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice salva = captor.getValue();
        assertThat(salva.getDocumentType()).isEqualTo(InvoiceType.NFE);
        assertThat(salva.getSaleId()).isEqualTo(12L);
        assertThat(salva.getStatus()).isEqualTo(NfseStatus.PROCESSANDO);
        assertThat(salva.getClientDocument()).isEqualTo(CPF_VALIDO);
        assertThat(salva.getValue()).isEqualByComparingTo("20.00");
        assertThat(r.type()).isEqualTo(InvoiceType.NFE);
        verifyNoInteractions(focus);
    }

    @Test
    void emit_cpfInvalido_recusaAntesDaSefaz() {
        when(saleRepository.findById(12L)).thenReturn(Optional.of(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.emit(pedidoBalcao("99999999999")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("CPF inválido");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void emit_semEndereco_dizExatamenteOQueFalta() {
        when(saleRepository.findById(12L)).thenReturn(Optional.of(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null))));
        NfeEmitRequest semEndereco = new NfeEmitRequest(12L, null, "João", CPF_VALIDO, null, null, null, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.emit(semEndereco));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("rua", "número", "bairro", "cidade", "UF", "CEP");
    }

    @Test
    void emit_documentoDeTamanhoErrado_pedeCpfOuCnpj() {
        when(saleRepository.findById(12L)).thenReturn(Optional.of(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.emit(pedidoBalcao("123")));

        assertThat(ex.getReason()).contains("CPF").contains("CNPJ");
    }

    @Test
    void emit_vendaCancelada_recusa() {
        when(saleRepository.findById(12L)).thenReturn(Optional.of(venda(SaleStatus.CANCELADA, PaymentMethod.PIX, produto(null, null, null, null))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.emit(pedidoBalcao(CPF_VALIDO)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void emit_vendaJaTemNotaAtiva_conflito() {
        when(saleRepository.findById(12L)).thenReturn(Optional.of(venda(SaleStatus.ATIVA, PaymentMethod.PIX, produto(null, null, null, null))));
        when(invoiceRepository.existsBySaleIdAndDocumentTypeAndStatusIn(any(), any(), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.emit(pedidoBalcao(CPF_VALIDO)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---------- production lock / configuration ----------

    @Test
    void pendencia_realSemToken_avisa() {
        nfse.setSimulate(false);

        assertThat(service.pendencia()).contains("Token da Focus NFe não configurado");
    }

    @Test
    void pendencia_producaoNaoConfirmada_bloqueia() {
        nfse.setSimulate(false);
        nfse.setToken("abc");
        nfse.setAmbiente("producao");
        nfse.setProducaoConfirmada(false);

        assertThat(service.pendencia()).contains("produção não confirmado");
    }

    @Test
    void pendencia_semInscricaoEstadual_avisa() {
        nfse.setSimulate(false);
        nfse.setToken("abc");
        nfe.setIeEmitente("");

        assertThat(service.pendencia()).contains("inscrição estadual");
    }

    @Test
    void pendencia_regimeNormal_aindaNaoSuportado() {
        nfse.setSimulate(false);
        nfse.setToken("abc");
        nfe.setRegimeTributario(3);

        assertThat(service.pendencia()).contains("Regime Normal");
    }

    @Test
    void pendencia_simulado_nuncaBloqueia() {
        assertThat(service.pendencia()).isNull();
    }

    // ---------- lookup / cancel ----------

    private Invoice notaNfe(NfseStatus status, LocalDateTime criadaEm) {
        return Invoice.builder().id(7L).documentType(InvoiceType.NFE).saleId(12L).reference("sale-12-1")
                .clientName("João").clientDocument(CPF_VALIDO).value(new BigDecimal("20.00"))
                .status(status).createdAt(criadaEm).build();
    }

    @Test
    void consult_simulado_autorizaDepoisDeAlgunsSegundos() {
        Invoice nota = notaNfe(NfseStatus.PROCESSANDO, LocalDateTime.now().minusSeconds(10));
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(nota));
        when(saleRepository.findById(12L)).thenReturn(Optional.empty());

        service.consult(7L);

        assertThat(nota.getStatus()).isEqualTo(NfseStatus.AUTORIZADO);
        assertThat(nota.getChaveAcesso()).hasSize(44).startsWith("26");
        assertThat(nota.getNumeroNfse()).isNotBlank();
        verifyNoInteractions(focus);
    }

    @Test
    void consult_simulado_recemCriadaContinuaProcessando() {
        Invoice nota = notaNfe(NfseStatus.PROCESSANDO, LocalDateTime.now());
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(nota));
        when(saleRepository.findById(12L)).thenReturn(Optional.empty());

        service.consult(7L);

        assertThat(nota.getStatus()).isEqualTo(NfseStatus.PROCESSANDO);
    }

    @Test
    void consult_naoENfe_recusa() {
        Invoice nfsE = Invoice.builder().id(9L).documentType(InvoiceType.NFSE).status(NfseStatus.AUTORIZADO).value(BigDecimal.TEN).build();
        when(invoiceRepository.findById(9L)).thenReturn(Optional.of(nfsE));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.consult(9L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancel_naoAutorizada_conflito() {
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(notaNfe(NfseStatus.PROCESSANDO, LocalDateTime.now())));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cancel(7L, "Cancelamento de teste - emitida por engano"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancel_justificativaCurta_recusa() {
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(notaNfe(NfseStatus.AUTORIZADO, LocalDateTime.now())));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.cancel(7L, "curto"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("15");
    }

    @Test
    void cancel_simulado_marcaComoCancelada() {
        Invoice nota = notaNfe(NfseStatus.AUTORIZADO, LocalDateTime.now());
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(nota));
        when(saleRepository.findById(12L)).thenReturn(Optional.empty());

        service.cancel(7L, "Cancelamento de teste - emitida por engano");

        assertThat(nota.getStatus()).isEqualTo(NfseStatus.CANCELADO);
        assertThat(nota.getCancelJustificativa()).contains("engano");
        assertThat(nota.getCancelledAt()).isNotNull();
        verifyNoInteractions(focus);
    }
}
