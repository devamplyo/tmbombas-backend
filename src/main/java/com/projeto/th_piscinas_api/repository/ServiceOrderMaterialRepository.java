package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.ServiceOrderMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceOrderMaterialRepository extends JpaRepository<ServiceOrderMaterial, Long> {

    List<ServiceOrderMaterial> findByServiceOrderIdOrderByCreatedAtAsc(Long serviceOrderId);
}
