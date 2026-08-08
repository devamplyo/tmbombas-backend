package com.projeto.th_piscinas_api.dto.externalSales;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalSellerSummary(
        Long sellerId,
        String sellerName,
        long salesCount,
        BigDecimal totalRevenue,
        LocalDateTime lastSale
) {
}
