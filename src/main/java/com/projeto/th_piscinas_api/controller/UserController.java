package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.auth.ResetPasswordRequest;
import com.projeto.th_piscinas_api.dto.auth.UserRequest;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> userList() {

        List<UserResponse> userList = userService.userList();

        return ResponseEntity.status(HttpStatus.OK).body(userList);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest req) {

        UserResponse createdUser = userService.createUser(req);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> editUser(@PathVariable Long id,
                                 @Valid @RequestBody UserRequest req) {

        UserResponse response = userService.editUser(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}/resetPassword")
    public ResponseEntity<String> resetPassword(@PathVariable Long id,
                                                @Valid
                                                @RequestBody
                                                ResetPasswordRequest req) {
        userService.resetPassword(id, req.newPassword());

        return ResponseEntity.ok("Senha redefinida com sucesso");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
