package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.auth.LoginRequest;
import com.projeto.th_piscinas_api.dto.auth.LoginResponse;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.mapper.UserMapper;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.security.JwtService;
import com.projeto.th_piscinas_api.security.RefreshTokenService;
import com.projeto.th_piscinas_api.security.TokenBlocklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlocklistService tokenBlocklistService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    public LoginResponse userLogin(LoginRequest req) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.matricula(), req.senha()));

        User user = userRepository.findByMatricula(req.matricula())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String token = jwtService.gerarToken(user);
        String refresh = refreshTokenService.createTokenRefresh(user.getMatricula());

        return new LoginResponse(
                token,
                refresh,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getNome(),
                user.getMatricula(),
                user.getPerfil().name()
        );
    }

    public UserResponse userProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }
        return userMapper.toUserResponse(user);
    }

    public LoginResponse refresh(String refreshToken) {

        String matricula = refreshTokenService.validateAndConsume(refreshToken); // rotation
        User user = userRepository.findByMatricula(matricula)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String access = jwtService.gerarToken(user);
        String novoRefresh = refreshTokenService.createTokenRefresh(matricula);

        return new LoginResponse(access, novoRefresh, "Bearer",
                jwtService.getExpirationSeconds(),
                user.getNome(), user.getMatricula(), user.getPerfil().name());
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            tokenBlocklistService.block(
                    jwtService.extrairJti(accessToken),
                    jwtService.extractExpiration(accessToken));
        }
        refreshTokenService.revoke(refreshToken);
    }
    }
