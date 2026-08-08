package com.projeto.th_piscinas_api.dto.externalorder;

import com.projeto.th_piscinas_api.util.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExternalOrderResponse(
        Long id, Long sellerId, String sellerName, String customerName,
        OrderStatus status, BigDecimal total, String notes, String rejectionReason,
        Long saleId, LocalDateTime createdAt,
        List<ExternalOrderItemResponse> items
) {
}
