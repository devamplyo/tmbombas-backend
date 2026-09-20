package com.projeto.th_piscinas_api.dto.invoice;

import com.projeto.th_piscinas_api.util.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A sale (NF-e) or a completed service order (NFS-e) that still has no active fiscal document.
 * {@code hasClient} is false for walk-in sales: the screen then asks for the recipient's data.
 */
public record PendingInvoiceResponse(
        InvoiceType type,
        Long originId,
        String originLabel,
        Long clientId,
        String clientName,
        String clientDocument,
        boolean hasClient,
        BigDecimal total,
        List<InvoiceItemResponse> items,
        LocalDateTime createdAt
) {
}
