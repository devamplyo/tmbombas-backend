package com.projeto.th_piscinas_api.dto.nfse;

/**
 * NFS-e configuration status — the front uses this to display state and a
 * pending-issue message, but the issue button stays always visible: the
 * real validation (and the handled 403 error) happens in POST /emit itself.
 */
public record NfseStatusResponse(
        boolean configured,
        String env,
        boolean simulate,
        boolean prontoParaReal,
        String pendencia
) {
}
