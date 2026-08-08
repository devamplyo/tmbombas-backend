package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.NfseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Issued NFS-e (service invoice) — entity independent from the Service Order.
 * {@code serviceOrderId} is just the opaque service order id in the front (no FK in Java).
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Our own unique reference (idempotency with Focus). */
    @Column(nullable = false, unique = true)
    private String reference;

    /** Service order id in the front (Node) — used to list/deduplicate. */
    @Column(name = "service_order_id")
    private String serviceOrderId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_document")
    private String clientDocument;

    @Column(name = "client_address")
    private String clientAddress;

    @Column(length = 1000)
    private String description;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NfseStatus status;

    @Column(name = "numero_nfse")
    private String numeroNfse;

    @Column(name = "url_pdf")
    private String urlPdf;

    @Column(name = "url_xml")
    private String urlXml;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** DPS series reported in this issuance (Nationwide NFS-e). */
    @Column(name = "serie_dps")
    private Integer serieDps;

    /** DPS number allocated by {@code DpsNumberAllocator} for this issuance. */
    @Column(name = "numero_dps")
    private Long numeroDps;

    /** Reference date of the service rendered (dCompet in the DPS). */
    @Column(name = "data_competencia")
    private LocalDate dataCompetencia;

    @Column(name = "cancel_justificativa", length = 500)
    private String cancelJustificativa;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.value == null) {
            this.value = BigDecimal.ZERO;
        }
    }
}
