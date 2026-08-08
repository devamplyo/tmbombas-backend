package com.projeto.th_piscinas_api.dto.nfse;

import jakarta.validation.constraints.NotNull;

/**
 * Data to issue an NFS-e. Just the service order id — client, value and
 * description are derived from {@code ServiceOrder}/{@code Client} in the
 * backend, not trusted from a snapshot sent by the front.
 */
public record NfseEmitRequest(
        @NotNull Long serviceOrderId
) {
}
