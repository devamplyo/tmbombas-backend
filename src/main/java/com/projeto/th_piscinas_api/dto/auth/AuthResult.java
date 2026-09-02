package com.projeto.th_piscinas_api.dto.auth;

/**
 * Carrega os ids opacos de sessão (pra virarem cookie no AuthController)
 * junto do corpo público da resposta — nunca o JWT/refresh token em si.
 */
public record AuthResult(String sessionId, String refreshId, LoginResponse body) {
}
