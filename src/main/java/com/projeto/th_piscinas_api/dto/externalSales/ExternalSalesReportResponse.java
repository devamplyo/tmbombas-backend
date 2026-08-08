package com.projeto.th_piscinas_api.dto.externalSales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExternalSalesReportResponse(
        List<ExternalSellerSummary> sellers,
        long totalSales,
        BigDecimal totalRevenue,
        LocalDate from,
        LocalDate to
) {
}
