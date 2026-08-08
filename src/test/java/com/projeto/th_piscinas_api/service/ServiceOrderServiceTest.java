package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderUpdateRequest;
import com.projeto.th_piscinas_api.exception.ClientNotFoundException;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.exception.ProfileNotValidateException;
import com.projeto.th_piscinas_api.mapper.ServiceOrderMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.ClientStatus;
import com.projeto.th_piscinas_api.util.ClientType;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ServiceOrderService serviceOrderService;

    private Client buildClient(Long id) {
        return Client.builder()
                .id(id)
                .name("Cliente Teste")
                .document("12345678901")
                .type(ClientType.PESSOA_FISICA)
                .status(ClientStatus.APROVADO)
                .active(true)
                .build();
    }

    private User buildTechnician(Long id) {
        return User.builder()
                .id(id)
                .nome("Tecnico")
                .matricula("TEC001")
                .senha("senha")
                .perfil(Perfil.TECNICO_CONDOMINIAL)
                .ativo(true)
                .build();
    }

    private User buildVendedor(Long id) {
        return User.builder()
                .id(id)
                .nome("Vendedor")
                .matricula("VND001")
                .senha("senha")
                .perfil(Perfil.VENDEDOR_INTERNO)
                .ativo(true)
                .build();
    }

    private ServiceOrder buildOrder(Long id, ServiceOrderStatus status) {
        return ServiceOrder.builder()
                .id(id)
                .client(buildClient(1L))
                .title("Manutenção Piscina")
                .description("Limpeza completa")
                .status(status)
                .build();
    }

    private ServiceOrderResponse buildResponse(Long id, ServiceOrderStatus status) {
        return new ServiceOrderResponse(id, 1L, "Cliente Teste", null, null,
                "Manutenção Piscina", null, "Limpeza completa", "OS-2026-0001", status,
                ServiceOrderType.OS, null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listOfServicesOrders_returnsMappedList() {
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.ABERTA);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.ABERTA);

        when(serviceOrderRepository.findAllWithClientTechnicianAndItems()).thenReturn(List.of(order));
        when(serviceOrderMapper.toResponse(order)).thenReturn(resp);

        List<ServiceOrderResponse> result = serviceOrderService.listOfServicesOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ServiceOrderStatus.ABERTA);
    }

    @Test
    void createServiceOrder_success_withoutTechnician() {

        ServiceOrderRequest req = new ServiceOrderRequest(1L, null, "Manutenção",
                "Descrição", null, null, null, null);
        Client client = buildClient(1L);
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.ABERTA);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.ABERTA);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(order);
        when(serviceOrderMapper.toResponse(order)).thenReturn(resp);



        ServiceOrderResponse result = serviceOrderService.createServiceOrder(req, null);

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.ABERTA);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createServiceOrder_success_withTechnician() {
        ServiceOrderRequest req = new ServiceOrderRequest(1L, 10L, "Manutenção",
                "Descrição", null, null, null, null);
        Client client = buildClient(1L);
        User tech = buildTechnician(10L);
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.ABERTA);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.ABERTA);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(10L)).thenReturn(Optional.of(tech));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(order);
        when(serviceOrderMapper.toResponse(order)).thenReturn(resp);

        ServiceOrderResponse result = serviceOrderService.createServiceOrder(req, null);

        assertThat(result).isNotNull();
        verify(userRepository).findById(10L);
    }

    @Test
    void createServiceOrder_throwsClientNotFoundException_whenClientNotExists() {
        ServiceOrderRequest req = new ServiceOrderRequest(99L, null, "Manutenção",
                "Descrição", null, null, null, null);

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> serviceOrderService.createServiceOrder(req, null));
        verify(serviceOrderRepository, never()).save(any());
    }

    @Test
    void createServiceOrder_throwsClientNotFoundException_whenTechnicianNotExists() {
        ServiceOrderRequest req = new ServiceOrderRequest(1L, 99L, "Manutenção",
                "Descrição", null, null, null, null);
        Client client = buildClient(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> serviceOrderService.createServiceOrder(req, null));
    }

    @Test
    void createServiceOrder_throwsProfileNotValidateException_whenUserIsNotTechnician() {
        ServiceOrderRequest req = new ServiceOrderRequest(1L, 10L, "Manutenção",
                "Descrição", null, null, null, null);
        Client client = buildClient(1L);
        User vendedor = buildVendedor(10L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(10L)).thenReturn(Optional.of(vendedor));

        assertThrows(ProfileNotValidateException.class,
                () -> serviceOrderService.createServiceOrder(req, null));
    }

    @Test
    void updateServiceOrder_success_updatesFields() {
        ServiceOrderUpdateRequest req = new ServiceOrderUpdateRequest(
                ServiceOrderStatus.EM_ANDAMENTO, null, "Nova desc",
                LocalDateTime.now().plusDays(1), new BigDecimal("250.00"));
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.ABERTA);
        ServiceOrder saved = buildOrder(1L, ServiceOrderStatus.EM_ANDAMENTO);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.EM_ANDAMENTO);

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(serviceOrderRepository.save(order)).thenReturn(saved);
        when(serviceOrderMapper.toResponse(saved)).thenReturn(resp);

        ServiceOrderResponse result = serviceOrderService.updateServiceOrder(1L, req);

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.EM_ANDAMENTO);
        assertThat(order.getDescription()).isEqualTo("Nova desc");
        assertThat(order.getPrice()).isEqualTo(new BigDecimal("250.00"));
    }

    @Test
    void updateServiceOrder_setsCompletedAt_whenStatusIsConcluida() {
        ServiceOrderUpdateRequest req = new ServiceOrderUpdateRequest(
                ServiceOrderStatus.CONCLUIDA, null, null, null, null);
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.EM_ANDAMENTO);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.CONCLUIDA);

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(serviceOrderRepository.save(order)).thenReturn(order);
        when(serviceOrderMapper.toResponse(order)).thenReturn(resp);

        serviceOrderService.updateServiceOrder(1L, req);

        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    void updateServiceOrder_doesNotOverwriteCompletedAt_whenAlreadySet() {
        ServiceOrderUpdateRequest req = new ServiceOrderUpdateRequest(
                ServiceOrderStatus.CONCLUIDA, null, null, null, null);
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.EM_ANDAMENTO);
        LocalDateTime originalCompletion = LocalDateTime.of(2026, 1, 1, 10, 0);
        order.setCompletedAt(originalCompletion);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.CONCLUIDA);

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(serviceOrderRepository.save(order)).thenReturn(order);
        when(serviceOrderMapper.toResponse(order)).thenReturn(resp);

        serviceOrderService.updateServiceOrder(1L, req);

        assertThat(order.getCompletedAt()).isEqualTo(originalCompletion);
    }

    @Test
    void updateServiceOrder_throwsOrderNotFoundException_whenNotExists() {
        ServiceOrderUpdateRequest req = new ServiceOrderUpdateRequest(
                ServiceOrderStatus.ABERTA, null, null, null, null);

        when(serviceOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> serviceOrderService.updateServiceOrder(99L, req));
    }

    @Test
    void updateServiceOrder_assignsTechnician_whenTechnicianIdProvided() {
        ServiceOrderUpdateRequest req = new ServiceOrderUpdateRequest(
                null, 10L, null, null, null);
        ServiceOrder order = buildOrder(1L, ServiceOrderStatus.ABERTA);
        User tech = buildTechnician(10L);
        ServiceOrderResponse resp = buildResponse(1L, ServiceOrderStatus.ABERTA);

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(tech));
        when(serviceOrderRepository.save(order)).thenReturn(order);
        when(serviceOrderMapper.toResponse(order)).thenReturn(resp);

        serviceOrderService.updateServiceOrder(1L, req);

        assertThat(order.getTechnician()).isEqualTo(tech);
    }
}
