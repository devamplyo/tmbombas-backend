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
import com.projeto.th_piscinas_api.util.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    public List<ClientResponse> listAllClients() {

        List<Client> clients = clientRepository.findAll();

        return clients.stream().map(clientMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse findClientById(Long id) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado: " + id));


        return clientMapper.toResponse(client);
    }

    @Transactional
    public ClientResponse createClient(ClientRequest req, User creator) {
        if (clientRepository.existsByDocument(req.document())) {
            throw new ConflictException(
                    "Já existe cliente com o documento " + req.document());
        }

        Client client = clientMapper.toEntity(req);
        client.setCreatedById(creator != null ? creator.getId() : null);

        boolean isAdmin = creator != null && creator.getPerfil() == Perfil.ADM_MASTER;
        if (isAdmin) {
            // ADM creates it already approved and active
            client.setStatus(ClientStatus.APROVADO);
            client.setActive(true);
            client.setApprovedById(creator.getId());
            client.setApprovedAt(LocalDateTime.now());
        } else {
            // salesperson: stays pending and inactive until the ADM approves
            client.setStatus(ClientStatus.PENDENTE);
            client.setActive(false);
        }

        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest req) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado: " + id));
        // if the document was changed, make sure it doesn't collide with another client
        if (!client.getDocument().equals(req.document())
                && clientRepository.existsByDocument(req.document())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe cliente com o documento " + req.document());
        }

        clientMapper.updateEntity(req, client);

        Client clientUpdated = clientRepository.save(client);

        return clientMapper.toResponse(clientUpdated);
    }


    @Transactional(readOnly = true)
    public List<ClientResponse> listPending() {

        List<Client> clientResponses = clientRepository.findByStatus(ClientStatus.PENDENTE);

        return clientResponses.stream().map(clientMapper::toResponse).toList();
    }

    @Transactional
    public ClientResponse approveClient(Long id, User admin) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(
                        "Cliente não encontrado: " + id));

        if (client.getStatus() != ClientStatus.PENDENTE) {
            throw new ClientNotPendingException(
                    "Cliente não está pendente de aprovação (status: " + client.getStatus() + ")");
        }
        client.setStatus(ClientStatus.APROVADO);
        client.setActive(true);
        client.setApprovedById(admin != null ? admin.getId() : null);
        client.setApprovedAt(LocalDateTime.now());
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse rejectClient(Long id, String reason) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(
                        "Cliente não encontrado: " + id));
        ;

        if (client.getStatus() != ClientStatus.PENDENTE) {
            throw new ClientNotPendingException(
                    "Cliente não está pendente de aprovação (status: " + client.getStatus() + ")");
        }
        client.setStatus(ClientStatus.REPROVADO);
        client.setActive(false);
        client.setRejectionReason(reason);
        return clientMapper.toResponse(clientRepository.save(client));
    }


}
