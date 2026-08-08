package com.projeto.th_piscinas_api.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String name,
        String cnpj,
        @Email String email,
        String phone
) {
}
