package com.projeto.th_piscinas_api.model;

import com.projeto.th_piscinas_api.util.OrigemLancamento;
import com.projeto.th_piscinas_api.util.TipoLancamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_launch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialLaunch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamento tipo;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemLancamento origem;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    /** Date that defines which period the entry falls into (inflow OR outflow). */
    @Column(nullable = false)
    private LocalDate data;

    private String descricao;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void onCreate() { this.criadoEm = LocalDateTime.now(); }
}
