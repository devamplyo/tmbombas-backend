package com.projeto.th_piscinas_api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ExternalSellerStatsProjection {

    Long getSellerId();
    Long getSalesCount();
    BigDecimal getTotalRevenue();
    LocalDateTime getLastSale();
}
