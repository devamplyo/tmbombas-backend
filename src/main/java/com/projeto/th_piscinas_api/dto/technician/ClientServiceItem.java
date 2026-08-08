package com.projeto.th_piscinas_api.dto.technician;

import java.time.LocalDateTime;

public record ClientServiceItem(
        Long orderId,
        String title,
        String status,
        LocalDateTime scheduledDate,
        LocalDateTime completedAt
) {
}
