package com.projeto.th_piscinas_api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Matrícula é obrigatória") String matricula,
        @NotBlank(message = "Senha é obrigatória") String senha
) {}
