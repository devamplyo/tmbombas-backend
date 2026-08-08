package com.projeto.th_piscinas_api.dto.auth;

public record LoginResponse(
        String token,
        String refresh,
        String tipo,
        long expiraEmSegundos,
        String nome,
        String matricula,
        String perfil
) {}
