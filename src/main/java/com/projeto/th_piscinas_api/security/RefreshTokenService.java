package com.projeto.th_piscinas_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redis;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshDays;

    /** Creates an opaque refresh token and stores it in Redis pointing to the registration number. */
    public String createTokenRefresh(String matricula) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(key(token), matricula, Duration.ofDays(refreshDays));
        return token;
    }

    /** Validates and CONSUMES it (single-use): if it exists, returns the registration number and deletes it. */
    public String validateAndConsume(String refreshToken) {
        String matricula = redis.opsForValue().get(key(refreshToken));
        if (matricula == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token inválido ou expirado");
        }
        redis.delete(key(refreshToken));   // rotation: can't be reused
        return matricula;
    }

    public void revoke(String refreshToken) {
        if (refreshToken != null) redis.delete(key(refreshToken));
    }

    private String key(String token) { return "refresh:" + token; }
}
