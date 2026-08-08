package com.projeto.th_piscinas_api.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        String code,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Integer availableStock,
        boolean stockOk
) {
}
