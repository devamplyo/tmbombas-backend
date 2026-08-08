package com.projeto.th_piscinas_api.dto.product;

public record LowStockResponse(
        Long id,
        String name,
        String code,
        Integer stock,
        Integer minStock,
        Integer shortage
) {
}
