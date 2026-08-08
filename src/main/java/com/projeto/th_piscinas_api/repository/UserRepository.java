package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.util.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    long countByPerfil(Perfil perfil);

    List<User> findByPerfil(Perfil perfil);

    Optional<User> findFirstByPerfil(Perfil perfil);


}
