package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.auth.LoginRequest;
import com.projeto.th_piscinas_api.dto.auth.LoginResponse;
import com.projeto.th_piscinas_api.dto.auth.RefreshRequest;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {

        LoginResponse loginResponse = authService.userLogin(req);

        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshRequest req) {

        LoginResponse response = authService.refresh(req.refreshToken());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserProfile(Authentication authentication) {

        UserResponse userProfile = authService.userProfile(authentication);

        return ResponseEntity.status(HttpStatus.OK).body(userProfile);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request,
                                       @RequestBody(required = false) RefreshRequest req) {
        String auth = request.getHeader("Authorization");
        String access = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        authService.logout(access, req != null ? req.refreshToken() : null);

        return ResponseEntity.noContent().build();
    }

}
