package com.projeto.th_piscinas_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Guarda o JWT de acesso no Redis, do lado do servidor (na VPS) — o
 * navegador só recebe um id de sessão aleatório, num cookie httpOnly.
 * JavaScript nunca enxerga o JWT em si, então nem um XSS conseguiria lê-lo
 * ou roubá-lo (diferente de antes, quando o token inteiro ficava em
 * localStorage). Mesmo padrão que RefreshTokenService já usava pro refresh
 * token, aplicado agora ao token de acesso também.
 */
@Service
@RequiredArgsConstructor
public class AccessSessionService {

    private final StringRedisTemplate redis;

    @Value("${JWT_EXPIRATION_MINUTES}")
    private long expirationMinutes;

    /** Guarda o JWT e devolve o id opaco de sessão que vai no cookie `sid`. */
    public String create(String jwt) {
        String sessionId = UUID.randomUUID().toString();
        redis.opsForValue().set(key(sessionId), jwt, Duration.ofMinutes(expirationMinutes));
        return sessionId;
    }

    /** Resolve o id de sessão para o JWT real — null se não existir ou já tiver expirado. */
    public String resolve(String sessionId) {
        if (sessionId == null) return null;
        return redis.opsForValue().get(key(sessionId));
    }

    public void revoke(String sessionId) {
        if (sessionId != null) redis.delete(key(sessionId));
    }

    private String key(String sessionId) { return "session:" + sessionId; }
}
