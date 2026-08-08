package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {
}
