package com.projeto.th_piscinas_api.dto.externalorder;

import java.math.BigDecimal;

public record ExternalOrderItemResponse(
        Long productId, String productName,
        Integer quantity, BigDecimal unitPrice, BigDecimal subtotal
) {
}
