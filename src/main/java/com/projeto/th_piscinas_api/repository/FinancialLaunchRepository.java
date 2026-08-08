package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.FinancialLaunch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinancialLaunchRepository extends JpaRepository<FinancialLaunch, Long> {

    /**
     * Entries for the period (inclusive). Aggregation by granularity
     * (day/week/month) is done in Java in the service — avoids the date_trunc
     * function, which is PostgreSQL-specific and doesn't run on the dev profile's H2.
     */
    List<FinancialLaunch> findByDataBetweenOrderByData(LocalDate inicio, LocalDate fim);

    @Query(value = """
        SELECT s.id            AS "supplierId",
               s.name          AS "supplierName",
               COALESCE(SUM(l.valor), 0) AS "totalSpent",
               COUNT(l.id)     AS "launchCount"
        FROM financial_launch l
        JOIN suppliers s ON s.id = l.supplier_id
        WHERE l.tipo = 'SAIDA'
          AND l.origem = 'FORNECEDOR'
          AND l.data BETWEEN :inicio AND :fim
        GROUP BY s.id, s.name
        ORDER BY "totalSpent" DESC
        """, nativeQuery = true)
    List<SupplierSpendingProjection> gastoPorFornecedor(@Param("inicio") LocalDate inicio,
                                                        @Param("fim") LocalDate fim);
}
