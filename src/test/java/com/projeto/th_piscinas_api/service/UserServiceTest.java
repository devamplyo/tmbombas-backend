package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.auth.UserRequest;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.exception.ConflictException;
import com.projeto.th_piscinas_api.exception.PasswordEmptyException;
import com.projeto.th_piscinas_api.exception.UserNotFoundException;
import com.projeto.th_piscinas_api.mapper.UserMapper;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id, String matricula, Perfil perfil) {
        return User.builder()
                .id(id)
                .nome("Usuario Teste")
                .matricula(matricula)
                .senha("encodedPassword")
                .perfil(perfil)
                .ativo(true)
                .build();
    }

    private UserResponse buildResponse(Long id, String matricula, Perfil perfil) {
        return new UserResponse(id, "Usuario Teste", matricula, perfil.name(), true);
    }

    @Test
    void userList_returnsAllUsersMapped() {
        User u = buildUser(1L, "MAT001", Perfil.VENDEDOR_INTERNO);
        UserResponse resp = buildResponse(1L, "MAT001", Perfil.VENDEDOR_INTERNO);

        when(userRepository.findAll()).thenReturn(List.of(u));
        when(userMapper.toUserResponse(u)).thenReturn(resp);

        List<UserResponse> result = userService.userList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).matricula()).isEqualTo("MAT001");
    }

    @Test
    void createUser_success() {
        UserRequest req = new UserRequest("Nome", "MAT001", "senha123", Perfil.VENDEDOR_INTERNO, true);
        User entity = buildUser(null, "MAT001", Perfil.VENDEDOR_INTERNO);
        User saved = buildUser(1L, "MAT001", Perfil.VENDEDOR_INTERNO);
        UserResponse resp = buildResponse(1L, "MAT001", Perfil.VENDEDOR_INTERNO);

        when(userRepository.existsByMatricula("MAT001")).thenReturn(false);
        when(userMapper.toDtoUser(req)).thenReturn(entity);
        when(passwordEncoder.encode("senha123")).thenReturn("encodedPassword");
        when(userRepository.save(entity)).thenReturn(saved);
        when(userMapper.toUserResponse(saved)).thenReturn(resp);

        UserResponse result = userService.createUser(req);

        assertThat(result.matricula()).isEqualTo("MAT001");
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(entity);
    }

    @Test
    void createUser_throwsConflictException_whenMatriculaExists() {
        UserRequest req = new UserRequest("Nome", "MAT001", "senha123", Perfil.VENDEDOR_INTERNO, true);

        when(userRepository.existsByMatricula("MAT001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_throwsPasswordEmptyException_whenSenhaIsNull() {
        UserRequest req = new UserRequest("Nome", "MAT001", null, Perfil.VENDEDOR_INTERNO, true);

        when(userRepository.existsByMatricula("MAT001")).thenReturn(false);

        assertThrows(PasswordEmptyException.class, () -> userService.createUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_throwsPasswordEmptyException_whenSenhaIsBlank() {
        UserRequest req = new UserRequest("Nome", "MAT001", "   ", Perfil.VENDEDOR_INTERNO, true);

        when(userRepository.existsByMatricula("MAT001")).thenReturn(false);

        assertThrows(PasswordEmptyException.class, () -> userService.createUser(req));
    }

    @Test
    void editUser_success_withNewPassword() {
        UserRequest req = new UserRequest("Nome Atualizado", "MAT001", "novaSenha", Perfil.ADM_MASTER, true);
        User existing = buildUser(1L, "MAT001", Perfil.VENDEDOR_INTERNO);
        User saved = buildUser(1L, "MAT001", Perfil.ADM_MASTER);
        UserResponse resp = buildResponse(1L, "MAT001", Perfil.ADM_MASTER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("novaSenha")).thenReturn("encodedNewPassword");
        when(userRepository.save(existing)).thenReturn(saved);
        when(userMapper.toUserResponse(saved)).thenReturn(resp);

        UserResponse result = userService.editUser(1L, req);

        assertThat(result.perfil()).isEqualTo(Perfil.ADM_MASTER.name());
        verify(passwordEncoder).encode("novaSenha");
    }

    @Test
    void editUser_success_withoutChangingPassword() {
        UserRequest req = new UserRequest("Nome Atualizado", "MAT001", null, Perfil.ADM_MASTER, false);
        User existing = buildUser(1L, "MAT001", Perfil.VENDEDOR_INTERNO);
        User saved = buildUser(1L, "MAT001", Perfil.ADM_MASTER);
        UserResponse resp = buildResponse(1L, "MAT001", Perfil.ADM_MASTER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(saved);
        when(userMapper.toUserResponse(saved)).thenReturn(resp);

        userService.editUser(1L, req);

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void editUser_throwsUserNotFoundException_whenNotExists() {
        UserRequest req = new UserRequest("Nome", "MAT001", "senha", Perfil.VENDEDOR_INTERNO, true);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.editUser(99L, req));
    }

    @Test
    void resetPassword_success() {
        User user = buildUser(1L, "MAT001", Perfil.VENDEDOR_INTERNO);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("novaSenha")).thenReturn("encodedNovaSenha");

        userService.resetPassword(1L, "novaSenha");

        assertThat(user.getSenha()).isEqualTo("encodedNovaSenha");
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_throwsUserNotFoundException_whenNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.resetPassword(99L, "senha"));
    }

    @Test
    void deleteUser_success() {
        User user = buildUser(1L, "MAT001", Perfil.VENDEDOR_INTERNO);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_throwsUserNotFoundException_whenNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any());
    }
}
