package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.PaymentMethod;
import com.projeto.th_piscinas_api.util.ReceivableSource;
import com.projeto.th_piscinas_api.util.ReceivableStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "receivables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Where the receivable amount came from (sale, service order, standalone). */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private ReceivableSource sourceType;

    /** Source id (e.g.: sale id). */
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "client_name")
    private String clientName;

    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Due date (to highlight overdue accounts). Optional. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    /** Financial entry generated when the receipt is confirmed. */
    @Column(name = "financial_launch_id")
    private Long financialLaunchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = ReceivableStatus.PENDENTE;
    }

}
