package com.projeto.th_piscinas_api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    private String description;

    /** How many days between each maintenance repetition. */
    @Min(1)
    @Column(name = "frequency_days", nullable = false)
    private Integer frequencyDays;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    /** Next scheduled maintenance. This is the field that drives the control panel. */
    @Column(name = "next_maintenance_date", nullable = false)
    private LocalDate nextMaintenanceDate;

    @Column(nullable = false)
    private Boolean active = true;


    @Builder.Default
    @Column(nullable = false)
    private Boolean released = false;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

}
