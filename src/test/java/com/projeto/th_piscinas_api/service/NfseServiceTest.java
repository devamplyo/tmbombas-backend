package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.config.NfseProperties;
import com.projeto.th_piscinas_api.dto.nfse.NfseEmitRequest;
import com.projeto.th_piscinas_api.dto.nfse.NfseResponse;
import com.projeto.th_piscinas_api.mapper.InvoiceMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.repository.InvoiceRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.util.ClientStatus;
import com.projeto.th_piscinas_api.util.ClientType;
import com.projeto.th_piscinas_api.util.NfseStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NfseServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private FocusNfeClient focusClient;

    @Mock
    private DpsNumberAllocator dpsNumberAllocator;

    private NfseProperties props;
    private NfseService nfseService;

    @BeforeEach
    void setUp() {
        props = new NfseProperties();
        nfseService = new NfseService(invoiceRepository, serviceOrderRepository, invoiceMapper, props, focusClient, dpsNumberAllocator);
    }

    private Client buildClient(String document) {
        return Client.builder()
                .id(1L)
                .name("Cliente Teste")
                .document(document)
                .type(ClientType.PESSOA_FISICA)
                .status(ClientStatus.APROVADO)
                .active(true)
                .build();
    }

    private ServiceOrder buildOrder(ServiceOrderStatus status, BigDecimal price, Client client) {
        return ServiceOrder.builder()
                .id(42L)
                .client(client)
                .title("Manutenção Piscina")
                .description("Limpeza completa")
                .status(status)
                .price(price)
                .build();
    }

    // ---------- validations before any external call ----------

    @Test
    void emit_osNaoEncontrada_lanca404() {
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void emit_osNaoConcluida_lanca400() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.ABERTA, new BigDecimal("100.00"), buildClient("12345678901"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("concluída");
    }

    @Test
    void emit_semPreco_lanca400() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.CONCLUIDA, null, buildClient("12345678901"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("valor");
    }

    @Test
    void emit_clienteSemDocumentoValido_lanca400() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.CONCLUIDA, new BigDecimal("100.00"), buildClient("123"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("CPF/CNPJ");
    }

    @Test
    void emit_notaJaAtiva_lanca409() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.CONCLUIDA, new BigDecimal("100.00"), buildClient("12345678901"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByServiceOrderIdAndStatusIn(eq("42"), anyList())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---------- Part 1's acceptance criteria: handled error without a token ----------

    @Test
    void emit_producaoSemToken_lanca403ComMensagemAcionavel() {
        props.setSimulate(false);
        props.setAmbiente("producao");
        props.setToken(""); // missing token

        ServiceOrder order = buildOrder(ServiceOrderStatus.CONCLUIDA, new BigDecimal("100.00"), buildClient("12345678901"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByServiceOrderIdAndStatusIn(eq("42"), anyList())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).contains("FOCUSNFE_TOKEN");
        verifyNoInteractions(focusClient);
        verifyNoInteractions(dpsNumberAllocator);
    }

    @Test
    void emit_producaoComTokenMasSemConfirmacao_lanca403() {
        props.setSimulate(false);
        props.setAmbiente("producao");
        props.setToken("um-token-qualquer");
        props.setProducaoConfirmada(false);

        ServiceOrder order = buildOrder(ServiceOrderStatus.CONCLUIDA, new BigDecimal("100.00"), buildClient("12345678901"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByServiceOrderIdAndStatusIn(eq("42"), anyList())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> nfseService.emit(new NfseEmitRequest(42L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).contains("NFSE_PRODUCAO_CONFIRMADA");
    }

    @Test
    void status_semToken_reportaPendenciaENaoProntoParaReal() {
        props.setSimulate(false);
        props.setToken("");

        var status = nfseService.status();

        assertThat(status.prontoParaReal()).isFalse();
        assertThat(status.pendencia()).contains("Token");
    }

    @Test
    void status_simulate_semPendencia() {
        props.setSimulate(true);

        var status = nfseService.status();

        assertThat(status.pendencia()).isNull();
        assertThat(status.simulate()).isTrue();
        // simulate is not "ready for real" - it's simulated, doesn't call Focus.
        assertThat(status.prontoParaReal()).isFalse();
    }

    // ---------- simulated happy path ----------

    @Test
    void emit_simulate_naoExigeToken() {
        props.setSimulate(true);

        ServiceOrder order = buildOrder(ServiceOrderStatus.CONCLUIDA, new BigDecimal("100.00"), buildClient("12345678901"));
        when(serviceOrderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByServiceOrderIdAndStatusIn(eq("42"), anyList())).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toResponse(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            return new NfseResponse(1L, i.getReference(), i.getServiceOrderId(), i.getClientName(),
                    i.getValue(), i.getStatus(), null, null, null, null, null);
        });

        NfseResponse response = nfseService.emit(new NfseEmitRequest(42L));

        assertThat(response.status()).isEqualTo(NfseStatus.PROCESSANDO);
        verifyNoInteractions(focusClient);
        verifyNoInteractions(dpsNumberAllocator);
    }
}
