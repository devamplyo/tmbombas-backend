package com.projeto.th_piscinas_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlocklistService {

    private final StringRedisTemplate redis;

    /** Blocks a jti only until the moment the token would have expired anyway. */
    public void block(String jti, Date expiration) {
        if (jti == null || expiration == null) return;
        long ttlMs = expiration.getTime() - System.currentTimeMillis();
        if (ttlMs > 0) {
            redis.opsForValue().set(key(jti), "1", Duration.ofMillis(ttlMs));
        }
    }

    public boolean isBlocked(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    private String key(String jti) { return "blocklist:" + jti; }
}
