package com.projeto.th_piscinas_api.dto.technician;


import java.time.LocalDateTime;

public record TechnicianActivityItem(
        Long orderId,
        String title,
        String clientName,
        String status,
        LocalDateTime scheduledDate,
        LocalDateTime scheduledEnd,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
