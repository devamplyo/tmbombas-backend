package com.projeto.th_piscinas_api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "Refresh token é obrigatório") String refreshToken
) {
}
