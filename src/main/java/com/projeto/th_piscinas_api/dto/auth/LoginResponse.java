package com.projeto.th_piscinas_api.dto.auth;

/**
 * Corpo devolvido por /login e /refresh. Não carrega mais `token`/`refresh`
 * — esses agora vão só em cookies httpOnly (ver AuthController), nunca no
 * JSON que o JavaScript do front consegue ler.
 */
public record LoginResponse(
        String nome,
        String matricula,
        String perfil
) {}
