package com.projeto.th_piscinas_api.dto.serviceOrder;

import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ServiceOrderResponse(
        Long id,
        Long clientId,
        String orderNumber,
        String clientName,
        Long technicianId,
        String technicianName,
        Long createdById,
        String title,
        String description,
        ServiceOrderStatus status,
        ServiceOrderType type,
        LocalDateTime scheduledDate,
        BigDecimal price,
        List<ServiceOrderItemResponse> items,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
