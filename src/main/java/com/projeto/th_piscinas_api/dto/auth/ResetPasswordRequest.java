package com.projeto.th_piscinas_api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @JsonProperty("newPassword")
        @NotBlank
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres") String newPassword
) {}
