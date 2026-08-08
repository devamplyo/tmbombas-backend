package com.projeto.th_piscinas_api.dto.stockentry;

import java.math.BigDecimal;

public record StockEntryItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal subtotal
) {
}
