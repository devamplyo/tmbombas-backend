package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCode(String code);

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND p.stock <= p.minStock
        ORDER BY (p.stock - p.minStock) ASC
        """)
    List<Product> findLowStock();

    Optional<Product> findByBarcodeAndActiveTrue(String barcode);

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(p.code) LIKE LOWER(CONCAT('%', :term, '%')))
        ORDER BY p.name ASC
        """)
    List<Product> searchActive(@Param("term") String term);
}
