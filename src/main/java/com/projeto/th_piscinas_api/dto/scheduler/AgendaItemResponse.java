package com.projeto.th_piscinas_api.dto.scheduler;

import java.time.LocalDateTime;

public record AgendaItemResponse(
        Long orderId,
        String title,
        String clientName,
        Long technicianId,
        String technicianName,
        LocalDateTime scheduledDate,
        LocalDateTime scheduledEnd,
        String status
) {
}
