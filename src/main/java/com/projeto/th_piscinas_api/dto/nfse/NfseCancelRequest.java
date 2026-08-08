package com.projeto.th_piscinas_api.dto.nfse;

import jakarta.validation.constraints.NotBlank;

public record NfseCancelRequest(
        @NotBlank String justificativa
) {
}
