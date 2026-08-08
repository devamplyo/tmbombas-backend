package com.projeto.th_piscinas_api.dto.product;

import java.math.BigDecimal;

public record ProductSearchResponse(
        Long id,
        String name,
        String code,
        String barcode,
        BigDecimal price,
        Integer stock
) {
}
