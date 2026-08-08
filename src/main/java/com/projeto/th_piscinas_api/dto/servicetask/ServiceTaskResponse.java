package com.projeto.th_piscinas_api.dto.servicetask;

import com.projeto.th_piscinas_api.util.ServiceTaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceTaskResponse(
        Long id,
        Long serviceOrderId,
        Long technicianId,
        String technicianName,
        Long clientId,
        String clientName,
        String description,
        ServiceTaskStatus status,
        LocalDate scheduledDate,
        String scheduledTime,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
