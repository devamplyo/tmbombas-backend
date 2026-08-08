package com.projeto.th_piscinas_api.dto.auth;

import com.projeto.th_piscinas_api.model.User;

public record UserResponse(
        Long id,
        String nome,
        String matricula,
        String perfil,
        boolean ativo
) {
//    public static UserResponse de(User u) {
//        return new UserResponse(u.getId(), u.getNome(), u.getMatricula(),
//                u.getPerfil().name(), u.isAtivo());
//    }
}
