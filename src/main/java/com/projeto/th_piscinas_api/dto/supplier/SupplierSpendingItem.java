package com.projeto.th_piscinas_api.dto.supplier;

import java.math.BigDecimal;

public record SupplierSpendingItem(
        Long supplierId,
        String supplierName,
        BigDecimal totalSpent,
        Long launchCount
) {
}
