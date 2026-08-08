package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOrder {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, updatable = false)
    private String orderNumber;

    /** Client where the service will be performed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Responsible technician (User with the TECNICO_CONDOMINIAL profile). Assigned later. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceOrderStatus status;

    /** Budget (multiple items, awaiting approval) or a direct Service Order. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceOrderType type;

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ServiceOrderItem> items = new ArrayList<>();

    /** Scheduled date/time for the service visit. */
    private LocalDateTime scheduledDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    /** Who created it (technician or external salesperson). Used so each profile only sees their own. */
    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** Price set by the ADM's pricing. Null while ABERTA (open). */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    private LocalDateTime completedAt;

    /** Links to the financial entry generated when the service order is completed and billed. */
    private Long financialLaunchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) this.status = ServiceOrderStatus.ABERTA;
        if (this.type == null) this.type = ServiceOrderType.OS;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

}
