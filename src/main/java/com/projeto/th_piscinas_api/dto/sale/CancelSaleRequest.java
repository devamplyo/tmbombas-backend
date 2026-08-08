package com.projeto.th_piscinas_api.dto.sale;

import jakarta.validation.constraints.NotBlank;

public record CancelSaleRequest(
        @NotBlank String adminMatricula,
        @NotBlank String adminPassword,
        @NotBlank(message = "Informe o motivo do cancelamento") String reason
) {
}
