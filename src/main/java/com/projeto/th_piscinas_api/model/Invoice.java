package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.InvoiceType;
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
 * Issued fiscal document, independent from the Service Order / Sale that originated it.
 * NFS-e (service): {@code serviceOrderId} is just the opaque service order id (no FK in Java).
 * NF-e (product): {@code saleId} is the sale id, and {@code chaveAcesso}/{@code protocolo}
 * come from SEFAZ. The column {@code numero_nfse} holds the document number of either type.
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

    /** NFS-e (service) or NF-e (product). Existing rows are NFS-e. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    private InvoiceType documentType = InvoiceType.NFSE;

    /** Sale that originated an NF-e (no FK, same convention as {@code serviceOrderId}). */
    @Column(name = "sale_id")
    private Long saleId;

    /** 44-digit access key of the NF-e (SEFAZ). */
    @Column(name = "chave_acesso", length = 60)
    private String chaveAcesso;

    /** Authorization protocol returned by SEFAZ. */
    @Column(length = 40)
    private String protocolo;

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
