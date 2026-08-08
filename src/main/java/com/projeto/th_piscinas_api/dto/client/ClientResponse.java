package com.projeto.th_piscinas_api.dto.client;

import com.projeto.th_piscinas_api.dto.address.AddressDto;
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
