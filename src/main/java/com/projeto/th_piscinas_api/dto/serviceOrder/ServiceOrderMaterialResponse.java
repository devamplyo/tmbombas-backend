package com.projeto.th_piscinas_api.dto.serviceOrder;

import java.math.BigDecimal;

public record ServiceOrderMaterialResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
