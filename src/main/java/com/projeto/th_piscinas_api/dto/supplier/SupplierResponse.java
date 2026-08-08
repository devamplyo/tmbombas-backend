package com.projeto.th_piscinas_api.dto.supplier;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String name,
        String cnpj,
        String email,
        String phone,
        Boolean active,
        LocalDateTime createdAt
) {
}
