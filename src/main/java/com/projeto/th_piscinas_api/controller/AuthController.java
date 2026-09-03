package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.auth.AuthResult;
import com.projeto.th_piscinas_api.dto.auth.LoginRequest;
import com.projeto.th_piscinas_api.dto.auth.LoginResponse;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.security.JwtAuthenticationFilter;
import com.projeto.th_piscinas_api.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * achado (pedido explícito: tirar a credencial do localStorage): o token de
 * acesso e o refresh token não trafegam mais no corpo JSON nem ficam
 * guardados no navegador de um jeito legível por JavaScript — só saem daqui
 * como cookies httpOnly com ids opacos (`sid`/`rid`). O valor de verdade
 * (o JWT) fica só no Redis, na VPS — ver AccessSessionService e
 * RefreshTokenService. Mesmo um XSS no front não conseguiria ler o token.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "rid";

    private final AuthService authService;

    @Value("${JWT_EXPIRATION_MINUTES}")
    private long accessMinutes;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshDays;

    /** false só no profile dev (HTTP puro em localhost) — ver application.yaml. */
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                                HttpServletResponse response) {

        AuthResult result = authService.userLogin(req);
        setCookies(response, result.sessionId(), result.refreshId());

        return ResponseEntity.status(HttpStatus.OK).body(result.body());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(HttpServletRequest request,
                                                        HttpServletResponse response) {

        String refreshId = extrairCookie(request, REFRESH_COOKIE);
        AuthResult result = authService.refresh(refreshId);
        setCookies(response, result.sessionId(), result.refreshId());

        return ResponseEntity.status(HttpStatus.OK).body(result.body());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserProfile(Authentication authentication) {

        UserResponse userProfile = authService.userProfile(authentication);

        return ResponseEntity.status(HttpStatus.OK).body(userProfile);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        String sessionId = extrairCookie(request, JwtAuthenticationFilter.SESSION_COOKIE);
        String refreshId = extrairCookie(request, REFRESH_COOKIE);
        authService.logout(sessionId, refreshId);
        clearCookies(response);

        return ResponseEntity.noContent().build();
    }

    private void setCookies(HttpServletResponse response, String sessionId, String refreshId) {
        addCookie(response, JwtAuthenticationFilter.SESSION_COOKIE, sessionId, Duration.ofMinutes(accessMinutes));
        addCookie(response, REFRESH_COOKIE, refreshId, Duration.ofDays(refreshDays));
    }

    private void clearCookies(HttpServletResponse response) {
        addCookie(response, JwtAuthenticationFilter.SESSION_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_COOKIE, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static String extrairCookie(HttpServletRequest request, String nome) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (nome.equals(c.getName())) return c.getValue();
        }
        return null;
    }

}
