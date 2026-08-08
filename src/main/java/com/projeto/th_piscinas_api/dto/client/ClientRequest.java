package com.projeto.th_piscinas_api.dto.client;

import com.projeto.th_piscinas_api.dto.address.AddressDto;
import com.projeto.th_piscinas_api.util.ClientType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotBlank String name,
        @NotBlank String document,
        @Email String email,
        String phone,
        @NotNull ClientType type,
        @Valid AddressDto address
) {
}
