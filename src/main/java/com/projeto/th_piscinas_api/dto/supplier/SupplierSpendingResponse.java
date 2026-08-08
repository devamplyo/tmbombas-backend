package com.projeto.th_piscinas_api.dto.supplier;

import java.math.BigDecimal;
import java.util.List;

public record SupplierSpendingResponse(
        List<SupplierSpendingItem> suppliers,
        BigDecimal totalSpent
) {
}
