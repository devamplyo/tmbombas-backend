package com.projeto.th_piscinas_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Autentica pelo cookie httpOnly `sid`, não mais pelo header Authorization
 * — o JWT em si nunca sai do servidor (fica no Redis, resolvido via
 * AccessSessionService; ver AuthController para onde o cookie é emitido).
 * O restante da validação (blocklist por jti, expiração, assinatura)
 * continua idêntico a antes.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Nome do cookie de sessão — usado aqui e em AuthController. */
    public static final String SESSION_COOKIE = "sid";

    private final JwtService jwtService;
    private final UserDetailsSvc userDetailsService;
    private final TokenBlocklistService tokenBlocklistService;
    private final AccessSessionService accessSessionService;

    @Override
    protected void doFilterInternal( HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        final String sessionId = extrairCookie(request, SESSION_COOKIE);
        final String token = accessSessionService.resolve(sessionId);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String matricula = jwtService.extrairMatricula(token);

            if (matricula != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails user = userDetailsService.loadUserByUsername(matricula);

                final String jti = jwtService.extrairJti(token);

                if (tokenBlocklistService.isBlocked(jti)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtService.tokenValido(token, user)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    static String extrairCookie(HttpServletRequest request, String nome) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (nome.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
