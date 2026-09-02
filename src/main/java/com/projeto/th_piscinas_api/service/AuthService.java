package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.auth.AuthResult;
import com.projeto.th_piscinas_api.dto.auth.LoginRequest;
import com.projeto.th_piscinas_api.dto.auth.LoginResponse;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.mapper.UserMapper;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.security.AccessSessionService;
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
    private final AccessSessionService accessSessionService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    /**
     * O token de acesso e o refresh token não voltam mais no corpo da
     * resposta — ficam no Redis (AccessSessionService/RefreshTokenService),
     * e o AuthController é quem transforma os ids opacos devolvidos aqui em
     * cookies httpOnly. Ver achado: credenciais saindo de localStorage.
     */
    public AuthResult userLogin(LoginRequest req) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.matricula(), req.senha()));

        User user = userRepository.findByMatricula(req.matricula())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String jwt = jwtService.gerarToken(user);
        String sessionId = accessSessionService.create(jwt);
        String refreshId = refreshTokenService.createTokenRefresh(user.getMatricula());

        LoginResponse body = new LoginResponse(user.getNome(), user.getMatricula(), user.getPerfil().name());
        return new AuthResult(sessionId, refreshId, body);
    }

    public UserResponse userProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }
        return userMapper.toUserResponse(user);
    }

    public AuthResult refresh(String refreshId) {

        String matricula = refreshTokenService.validateAndConsume(refreshId); // rotation
        User user = userRepository.findByMatricula(matricula)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String jwt = jwtService.gerarToken(user);
        String sessionId = accessSessionService.create(jwt);
        String novoRefreshId = refreshTokenService.createTokenRefresh(matricula);

        LoginResponse body = new LoginResponse(user.getNome(), user.getMatricula(), user.getPerfil().name());
        return new AuthResult(sessionId, novoRefreshId, body);
    }

    /** sessionId/refreshId são os ids opacos lidos dos cookies `sid`/`rid`. */
    public void logout(String sessionId, String refreshId) {
        String jwt = accessSessionService.resolve(sessionId);
        if (jwt != null) {
            tokenBlocklistService.block(jwtService.extrairJti(jwt), jwtService.extractExpiration(jwt));
        }
        accessSessionService.revoke(sessionId);
        refreshTokenService.revoke(refreshId);
    }
    }
