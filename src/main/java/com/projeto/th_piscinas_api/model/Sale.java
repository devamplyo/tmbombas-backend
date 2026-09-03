package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.PaymentMethod;
import com.projeto.th_piscinas_api.util.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    /** Who recorded the sale (registration number/id of the logged-in salesperson). */
    private Long sellerId;

    private String sellerName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /** Links the sale to the generated financial entry, to reverse it on cancellation. */
    private Long financialLaunchId;

    /** How the customer paid (achado F3) — informational, echoed back to the UI. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Builder.Default
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status = SaleStatus.ATIVA;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    /** Which ADM authorized the cancellation (entered the password). */
    @Column(name = "authorized_by_admin_id")
    private Long authorizedByAdminId;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = SaleStatus.ATIVA;
    }

    public void addItem(SaleItem item) {
        item.setSale(this);
        this.items.add(item);
    }
}
