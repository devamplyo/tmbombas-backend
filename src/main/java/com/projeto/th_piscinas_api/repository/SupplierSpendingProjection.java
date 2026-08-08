package com.projeto.th_piscinas_api.repository;

import java.math.BigDecimal;

public interface SupplierSpendingProjection {
    Long getSupplierId();
    String getSupplierName();
    BigDecimal getTotalSpent();
    Long getLaunchCount();
}
