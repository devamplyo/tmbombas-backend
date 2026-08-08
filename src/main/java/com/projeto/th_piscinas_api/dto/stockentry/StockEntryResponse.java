package com.projeto.th_piscinas_api.dto.stockentry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StockEntryResponse(
        Long id,
        Long supplierId,
        String supplierName,
        String documentNumber,
        LocalDate entryDate,
        BigDecimal total,
        List<StockEntryItemResponse> items
) {
}
