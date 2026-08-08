package com.projeto.th_piscinas_api.dto.nfse;

import com.projeto.th_piscinas_api.util.NfseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * NFS-e returned to the front. JSON in snake_case (numero_nfse, url_pdf,
 * url_xml, error_message) — exactly what NfseSection consumes.
 */
public record NfseResponse(
        Long id,
        String reference,
        String serviceOrderId,
        String clientName,
        BigDecimal value,
        NfseStatus status,
        String numeroNfse,
        String urlPdf,
        String urlXml,
        String errorMessage,
        LocalDateTime createdAt
) {
}
