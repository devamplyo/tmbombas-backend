package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.util.InvoiceType;
import com.projeto.th_piscinas_api.util.NfseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByServiceOrderIdOrderByCreatedAtDesc(String serviceOrderId);

    boolean existsByServiceOrderIdAndStatusIn(String serviceOrderId, Collection<NfseStatus> statuses);

    /** Blocks a second NF-e for the same sale while one is processing or authorized. */
    boolean existsBySaleIdAndDocumentTypeAndStatusIn(Long saleId, InvoiceType documentType, Collection<NfseStatus> statuses);

    /** Most recent documents of both types, for the "Notas Fiscais" screen. */
    List<Invoice> findTop500ByOrderByCreatedAtDesc();
}
