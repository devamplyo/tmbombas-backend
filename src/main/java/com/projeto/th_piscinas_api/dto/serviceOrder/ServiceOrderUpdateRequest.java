package com.projeto.th_piscinas_api.dto.serviceOrder;

import com.projeto.th_piscinas_api.util.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceOrderUpdateRequest(
        ServiceOrderStatus status,
        Long technicianId,
        String description,
        LocalDateTime scheduledDate,
        BigDecimal price
) {
}
