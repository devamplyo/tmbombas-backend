package com.projeto.th_piscinas_api.security;

import com.projeto.th_piscinas_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@NullMarked
@RequiredArgsConstructor
public class UserDetailsSvc implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String registration) {
        return userRepository.findByMatricula(registration)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado: " + registration));
    }
}
