package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    List<ServiceRecord> findByServiceOrderIdOrderByCreatedAtDesc(Long serviceOrderId);
}
