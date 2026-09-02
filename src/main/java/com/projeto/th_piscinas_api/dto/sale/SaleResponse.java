package com.projeto.th_piscinas_api.dto.sale;


import com.projeto.th_piscinas_api.util.PaymentMethod;
import com.projeto.th_piscinas_api.util.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        String customerName,
        Long sellerId,
        String sellerName,
        BigDecimal total,
        LocalDateTime createdAt,
        SaleStatus status,
        PaymentMethod paymentMethod,
        List<SaleItemResponse> items
) {
}
