package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.client.ClientRequest;
import com.projeto.th_piscinas_api.dto.client.ClientResponse;
import com.projeto.th_piscinas_api.dto.serviceOrder.RejectRequest;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO','VENDEDOR_EXTERNO','TECNICO_CONDOMINIAL')")
public class ClientController {

    private final ClientService clientService;


    @GetMapping
    public ResponseEntity<List<ClientResponse>> listOfClients() {

        List<ClientResponse> clients = clientService.listAllClients();

        return ResponseEntity.status(HttpStatus.OK).body(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> findClientById(@PathVariable Long id) {

        ClientResponse client = clientService.findClientById(id);

        return ResponseEntity.status(HttpStatus.OK).body(client);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid
                                                       @RequestBody ClientRequest req,
                                                       @AuthenticationPrincipal User creator
    ) {
        ClientResponse client = clientService.createClient(req, creator);

        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }

    // revisão de permissões: editar cliente herdava a trava da classe (os 4
    // papéis), então Técnico e Vendedor Externo editavam dados de qualquer
    // cliente. Só o ADM Master edita pela tela (ClientsPage) — restringido
    // pra isso; criar cliente continua liberado pros vendedores.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable Long id,
                                                       @Valid @RequestBody ClientRequest req) {

        ClientResponse client = clientService.updateClient(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(client);

    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<List<ClientResponse>> pendingClient() {

        List<ClientResponse> response = clientService.listPending();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<ClientResponse> approveClient(@PathVariable Long id,
                                                        @AuthenticationPrincipal User admin) {

        ClientResponse response = clientService.approveClient(id, admin);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<ClientResponse> rejectClient(@PathVariable Long id,
                                                       @RequestBody RejectRequest req) {

        ClientResponse response = clientService.rejectClient(id, req.reason());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
