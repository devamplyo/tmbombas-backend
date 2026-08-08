package com.projeto.th_piscinas_api.dto.serviceOrder;

import com.projeto.th_piscinas_api.util.ServiceOrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ServiceOrderRequest(
        @NotNull Long clientId,
        Long technicianId,
        @NotBlank String title,
        String description,
        LocalDateTime scheduledDate,
        BigDecimal price,
        ServiceOrderType type,
        List<ServiceOrderItemRequest> items
) {
}
