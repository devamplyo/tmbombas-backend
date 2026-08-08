package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "external_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalOrder {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;
    private String sellerName;
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** Estimated total (price snapshot at the time of the order). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    private String notes;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "processed_by_id")
    private Long processedById;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** Sale generated when the ADM approves. */
    @Column(name = "sale_id")
    private Long saleId;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalOrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = OrderStatus.ENVIADO;
    }

    public void addItem(ExternalOrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }
}
