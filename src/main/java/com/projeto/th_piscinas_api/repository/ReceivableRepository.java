package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Receivable;
import com.projeto.th_piscinas_api.util.ReceivableSource;
import com.projeto.th_piscinas_api.util.ReceivableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    List<Receivable> findByStatus(ReceivableStatus status);

    Optional<Receivable> findBySourceTypeAndSourceId(ReceivableSource sourceType, Long sourceId);

}
