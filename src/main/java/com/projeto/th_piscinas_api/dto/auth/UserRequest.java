package com.projeto.th_piscinas_api.dto.auth;

import com.projeto.th_piscinas_api.util.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record UserRequest(
        @NotBlank(message = "Informe o nome do usuário") String nome,
        @NotBlank(message = "Digite a matricula") String matricula,
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres") String senha,
        @NotNull(message = "Informe o perfil do usuário") Perfil perfil,
        Boolean ativo
) {
}
