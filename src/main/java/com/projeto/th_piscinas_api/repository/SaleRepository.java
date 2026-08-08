package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
        SELECT s.sellerId AS sellerId,
               COUNT(s)   AS salesCount,
               COALESCE(SUM(s.total), 0) AS totalRevenue,
               MAX(s.createdAt) AS lastSale
        FROM Sale s
        WHERE s.sellerId IS NOT NULL
        GROUP BY s.sellerId
        """)
    List<SellerStatsProjection> aggregateBySeller();

    // no SaleRepository
    @Query("""
        SELECT s.sellerId AS sellerId,
               COUNT(s)    AS salesCount,
               COALESCE(SUM(s.total), 0) AS totalRevenue,
               MAX(s.createdAt) AS lastSale
        FROM Sale s
        WHERE s.sellerId IN :sellerIds
          AND s.createdAt BETWEEN :from AND :to
        GROUP BY s.sellerId
        """)
    List<ExternalSellerStatsProjection> aggregateBySellers(@Param("sellerIds") List<Long> sellerIds,
                                                           @Param("from") LocalDateTime from,
                                                           @Param("to") LocalDateTime to);

    List<Sale> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
