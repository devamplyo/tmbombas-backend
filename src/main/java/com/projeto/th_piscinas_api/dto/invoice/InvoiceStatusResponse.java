package com.projeto.th_piscinas_api.dto.invoice;

/**
 * Issuing environment, for the banner of the screen: {@code env} is "homologacao" or "producao";
 * {@code simulate} means nothing is sent to Focus (local/test mode).
 */
public record InvoiceStatusResponse(
        String env,
        boolean simulate,
        boolean readyForReal,
        String pendencia
) {
}
