package com.projeto.th_piscinas_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_EXPIRATION_MINUTES}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMinutes * 60 * 1000;
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    public String gerarToken(UserDetails user) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .claim("perfil", user.getAuthorities().stream()
                        .findFirst().map(Object::toString).orElse(""))
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + expirationMillis))
                .signWith(key)
                .compact();
    }

    public String extrairMatricula(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, UserDetails user) {
        final String matricula = extrairMatricula(token);
        return matricula.equals(user.getUsername()) && !expirado(token);
    }

    private boolean expirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    public String extrairJti(String token) {
        return extrairClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }


    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
