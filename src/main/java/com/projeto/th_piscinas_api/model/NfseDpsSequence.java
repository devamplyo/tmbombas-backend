package com.projeto.th_piscinas_api.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * numero_dps counter per (cnpj, serie) — Focus doesn't generate this number,
 * the issuer provides it, and a repeated number is rejected by the city.
 */
@Entity
@Table(name = "nfse_dps_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfseDpsSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private Integer serie;

    @Column(name = "ultimo_numero", nullable = false)
    private Long ultimoNumero;
}
