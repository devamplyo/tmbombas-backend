package com.projeto.th_piscinas_api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyAdminRequest(
        @NotBlank(message = "Senha é obrigatória") String password
) {}
