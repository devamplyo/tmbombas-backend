package com.projeto.th_piscinas_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers( "/api/auth/logout", "/api/auth/me")
                                .authenticated()
                        .requestMatchers("/api/products/**").authenticated()
                        .requestMatchers("/api/suppliers/**").authenticated()
                        .requestMatchers("/api/serviceorders/**").authenticated()
                        .requestMatchers("/api/clients/**").authenticated()
                        .requestMatchers("/api/sales/**").authenticated()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        // used to be a single pattern with a comma inside (never matched); now split up
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                                .permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADM_MASTER")
                        .requestMatchers("/api/admin/maintenance-plans/**")
                        .hasAnyRole("ADM_MASTER", "VENDEDOR_INTERNO")
                        .requestMatchers("/api/admin/**").hasRole("ADM_MASTER")
                        .requestMatchers("/api/nfse/**").hasRole("ADM_MASTER")
                        .requestMatchers("/api/technician/**").hasAnyRole("ADM_MASTER", "TECNICO_CONDOMINIAL")
                        .requestMatchers("/api/servicetasks/**").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/external-orders/**")
                        .hasAnyRole("ADM_MASTER", "VENDEDOR_EXTERNO")
                        .requestMatchers("/api/stock-entries/**").authenticated()
                        .requestMatchers("/vendas/**")
                        .hasAnyRole("ADM_MASTER", "VENDEDOR_INTERNO", "VENDEDOR_EXTERNO")
                        .requestMatchers("/tecnico/**")
                        .hasAnyRole("ADM_MASTER", "TECNICO_CONDOMINIAL")
                        // deny-by-default: a new route without an explicit matcher requires
                        // login, instead of being born public
                        .anyRequest().authenticated()
                )
//            .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
