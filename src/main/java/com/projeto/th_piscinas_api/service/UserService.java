package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.auth.UserRequest;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.exception.ConflictException;
import com.projeto.th_piscinas_api.exception.PasswordEmptyException;
import com.projeto.th_piscinas_api.exception.UserNotFoundException;
import com.projeto.th_piscinas_api.mapper.UserMapper;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public List<UserResponse> userList() {

        List<User> userList = userRepository.findAll();

        return userList.stream().map(userMapper::toUserResponse).collect(Collectors.toList());
    }

    public UserResponse createUser(UserRequest req) {
        if (userRepository.existsByMatricula(req.matricula())) {
            throw new ConflictException("Já existe usuário com a matrícula " + req.matricula());
        }
        if (req.senha() == null || req.senha().isBlank()) {
            throw new PasswordEmptyException("Senha é obrigatória ao criar usuário");
        }

        User createdUser = userMapper.toDtoUser(req);
        createdUser.setSenha(Objects.requireNonNull(passwordEncoder.encode(req.senha())));
        User userResponse = userRepository.save(createdUser);

        return userMapper.toUserResponse(userResponse);
    }

    public UserResponse editUser(Long id, UserRequest req) {
       User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado: " + id));
        user.setNome(req.nome());
        user.setPerfil(req.perfil());
        if (req.ativo() != null) user.setAtivo(req.ativo());
        if (req.senha() != null && !req.senha().isBlank()) {
            user.setSenha(Objects.requireNonNull(passwordEncoder.encode(req.senha())));
        }

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado: " + id));

        user.setSenha(Objects.requireNonNull(passwordEncoder.encode(newPassword)));
         userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado: " + id));

        userRepository.delete(user);
    }


}
