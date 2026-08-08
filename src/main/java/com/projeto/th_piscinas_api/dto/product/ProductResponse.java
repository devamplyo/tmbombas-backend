package com.projeto.th_piscinas_api.dto.product;

import com.projeto.th_piscinas_api.util.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String code,
        String barcode,
        String manufacturer,
        BigDecimal price,
        Integer stock,
        Integer minStock,
        ProductCategory category,
        Double powerHp,
        Double maxFlowRate,
        Integer voltage,
        String unit,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
