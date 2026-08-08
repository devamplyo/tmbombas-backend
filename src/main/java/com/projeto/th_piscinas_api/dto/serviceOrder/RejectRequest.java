package com.projeto.th_piscinas_api.dto.serviceOrder;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank(message = "Informe o motivo da reprovação") String reason
) {
}
