package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.client.ClientRequest;
import com.projeto.th_piscinas_api.dto.client.ClientResponse;
import com.projeto.th_piscinas_api.exception.ClientNotFoundException;
import com.projeto.th_piscinas_api.exception.ClientNotPendingException;
import com.projeto.th_piscinas_api.exception.ConflictException;
import com.projeto.th_piscinas_api.mapper.ClientMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.util.ClientStatus;
import com.projeto.th_piscinas_api.util.ClientType;
import com.projeto.th_piscinas_api.util.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private Client buildClient(Long id, String document, ClientStatus status, boolean active) {
        return Client.builder()
                .id(id)
                .name("Cliente Teste")
                .document(document)
                .type(ClientType.PESSOA_FISICA)
                .status(status)
                .active(active)
                .build();
    }

    private ClientResponse buildResponse(Long id, String document) {
        return new ClientResponse(id, "Cliente Teste", document, null, null,
                ClientType.PESSOA_FISICA, null, true, null, null);
    }

    private ClientRequest buildRequest(String document) {
        return new ClientRequest("Cliente Teste", document, null, null, ClientType.PESSOA_FISICA, null);
    }

    private User buildAdmin() {
        return User.builder()
                .id(1L)
                .nome("Admin")
                .matricula("ADM001")
                .senha("senha")
                .perfil(Perfil.ADM_MASTER)
                .ativo(true)
                .build();
    }

    private User buildVendedor() {
        return User.builder()
                .id(2L)
                .nome("Vendedor")
                .matricula("VND001")
                .senha("senha")
                .perfil(Perfil.VENDEDOR_INTERNO)
                .ativo(true)
                .build();
    }

    @Test
    void listAllClients_returnsMappedList() {
        Client c = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.findAll()).thenReturn(List.of(c));
        when(clientMapper.toResponse(c)).thenReturn(resp);

        List<ClientResponse> result = clientService.listAllClients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).document()).isEqualTo("12345678901");
    }

    @Test
    void findClientById_success() {
        Client c = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(clientMapper.toResponse(c)).thenReturn(resp);

        ClientResponse result = clientService.findClientById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findClientById_throwsClientNotFoundException_whenNotExists() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.findClientById(99L));
    }

    @Test
    void createClient_asAdmin_createsApprovedAndActive() {
        ClientRequest req = buildRequest("12345678901");
        User admin = buildAdmin();
        Client entity = buildClient(null, "12345678901", null, false);
        Client saved = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.existsByDocument("12345678901")).thenReturn(false);
        when(clientMapper.toEntity(req)).thenReturn(entity);
        when(clientRepository.save(entity)).thenReturn(saved);
        when(clientMapper.toResponse(saved)).thenReturn(resp);

        ClientResponse result = clientService.createClient(req, admin);

        assertThat(entity.getStatus()).isEqualTo(ClientStatus.APROVADO);
        assertThat(entity.getActive()).isTrue();
        assertThat(entity.getApprovedById()).isEqualTo(1L);
        assertThat(result).isNotNull();
    }

    @Test
    void createClient_asVendedor_createsPendingAndInactive() {
        ClientRequest req = buildRequest("12345678901");
        User vendedor = buildVendedor();
        Client entity = buildClient(null, "12345678901", null, false);
        Client saved = buildClient(1L, "12345678901", ClientStatus.PENDENTE, false);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.existsByDocument("12345678901")).thenReturn(false);
        when(clientMapper.toEntity(req)).thenReturn(entity);
        when(clientRepository.save(entity)).thenReturn(saved);
        when(clientMapper.toResponse(saved)).thenReturn(resp);

        clientService.createClient(req, vendedor);

        assertThat(entity.getStatus()).isEqualTo(ClientStatus.PENDENTE);
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void createClient_throwsConflictException_whenDocumentExists() {
        ClientRequest req = buildRequest("12345678901");

        when(clientRepository.existsByDocument("12345678901")).thenReturn(true);

        assertThrows(ConflictException.class, () -> clientService.createClient(req, buildAdmin()));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClient_success() {
        ClientRequest req = buildRequest("12345678901");
        Client existing = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);
        Client saved = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(saved);
        when(clientMapper.toResponse(saved)).thenReturn(resp);

        ClientResponse result = clientService.updateClient(1L, req);

        assertThat(result).isNotNull();
        verify(clientMapper).updateEntity(req, existing);
    }

    @Test
    void updateClient_throwsConflictException_whenDocumentBelongsToAnotherClient() {
        ClientRequest req = buildRequest("99999999999");
        Client existing = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByDocument("99999999999")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> clientService.updateClient(1L, req));
    }

    @Test
    void updateClient_throwsClientNotFoundException_whenNotExists() {
        ClientRequest req = buildRequest("12345678901");

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.updateClient(99L, req));
    }

    @Test
    void listPending_returnsPendingClients() {
        Client c = buildClient(1L, "12345678901", ClientStatus.PENDENTE, false);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.findByStatus(ClientStatus.PENDENTE)).thenReturn(List.of(c));
        when(clientMapper.toResponse(c)).thenReturn(resp);

        List<ClientResponse> result = clientService.listPending();

        assertThat(result).hasSize(1);
    }

    @Test
    void approveClient_success() {
        User admin = buildAdmin();
        Client client = buildClient(1L, "12345678901", ClientStatus.PENDENTE, false);
        Client saved = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(saved);
        when(clientMapper.toResponse(saved)).thenReturn(resp);

        clientService.approveClient(1L, admin);

        assertThat(client.getStatus()).isEqualTo(ClientStatus.APROVADO);
        assertThat(client.getActive()).isTrue();
        assertThat(client.getApprovedById()).isEqualTo(admin.getId());
        assertThat(client.getApprovedAt()).isNotNull();
    }

    @Test
    void approveClient_throwsClientNotFoundException_whenNotExists() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> clientService.approveClient(99L, buildAdmin()));
    }

    @Test
    void approveClient_throwsClientNotPendingException_whenAlreadyApproved() {
        Client client = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThrows(ClientNotPendingException.class,
                () -> clientService.approveClient(1L, buildAdmin()));
    }

    @Test
    void rejectClient_success() {
        Client client = buildClient(1L, "12345678901", ClientStatus.PENDENTE, false);
        Client saved = buildClient(1L, "12345678901", ClientStatus.REPROVADO, false);
        ClientResponse resp = buildResponse(1L, "12345678901");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(saved);
        when(clientMapper.toResponse(saved)).thenReturn(resp);

        clientService.rejectClient(1L, "Documentação inválida");

        assertThat(client.getStatus()).isEqualTo(ClientStatus.REPROVADO);
        assertThat(client.getActive()).isFalse();
        assertThat(client.getRejectionReason()).isEqualTo("Documentação inválida");
    }

    @Test
    void rejectClient_throwsClientNotFoundException_whenNotExists() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> clientService.rejectClient(99L, "motivo"));
    }

    @Test
    void rejectClient_throwsClientNotPendingException_whenNotPending() {
        Client client = buildClient(1L, "12345678901", ClientStatus.APROVADO, true);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThrows(ClientNotPendingException.class,
                () -> clientService.rejectClient(1L, "motivo"));
    }
}
