package com.projeto.th_piscinas_api.dto.invoice;

import com.projeto.th_piscinas_api.util.InvoiceType;
import com.projeto.th_piscinas_api.util.NfseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A fiscal document (NF-e or NFS-e) as the "Notas Fiscais" screen consumes it. JSON in
 * snake_case. {@code urlPdf}/{@code urlXml} are always links the browser can open directly
 * (for NF-e they point at our own {@code /documento} endpoint, which fetches the file from Focus).
 */
public record InvoiceResponse(
        Long id,
        InvoiceType type,
        String reference,
        Long saleId,
        String serviceOrderId,
        String clientName,
        String clientDocument,
        String description,
        BigDecimal value,
        NfseStatus status,
        String number,
        String chaveAcesso,
        String protocolo,
        String urlPdf,
        String urlXml,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        List<InvoiceItemResponse> items
) {
}
