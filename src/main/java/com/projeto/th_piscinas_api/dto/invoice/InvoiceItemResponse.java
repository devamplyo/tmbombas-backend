package com.projeto.th_piscinas_api.dto.invoice;

import java.math.BigDecimal;

/** One line of a document, for display. */
public record InvoiceItemResponse(
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
