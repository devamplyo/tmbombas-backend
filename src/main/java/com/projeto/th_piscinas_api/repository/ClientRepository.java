package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.util.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByDocument(String document);

    List<Client> findByStatus(ClientStatus status);

    List<Client> findByActiveTrue();
}
