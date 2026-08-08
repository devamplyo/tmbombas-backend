package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByCnpj(String cnpj);
}
