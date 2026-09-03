package com.projeto.th_piscinas_api.dto.client;

import com.projeto.th_piscinas_api.dto.address.AddressDto;
import com.projeto.th_piscinas_api.util.ClientStatus;
import com.projeto.th_piscinas_api.util.ClientType;

import java.time.LocalDateTime;

public record ClientResponse(
        Long id,
        String name,
        String document,
        String email,
        String phone,
        ClientType type,
        AddressDto address,
        Boolean active,
        // achado (descoberto ao corrigir F14): faltava esse campo — o front só
        // tinha `active` pra decidir o badge/botões, e PENDENTE e REPROVADO são
        // os dois active=false, indistinguíveis. Rejeitar "funcionava" no
        // backend, mas a tela nunca conseguia mostrar isso.
        ClientStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
