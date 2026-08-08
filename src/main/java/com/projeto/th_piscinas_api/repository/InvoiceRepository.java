package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.util.NfseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByServiceOrderIdOrderByCreatedAtDesc(String serviceOrderId);

    boolean existsByServiceOrderIdAndStatusIn(String serviceOrderId, Collection<NfseStatus> statuses);
}
