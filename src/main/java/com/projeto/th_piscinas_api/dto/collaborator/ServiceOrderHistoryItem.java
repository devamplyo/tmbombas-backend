package com.projeto.th_piscinas_api.dto.collaborator;

import java.time.LocalDateTime;

public record ServiceOrderHistoryItem(
        Long orderId, String title, String clientName,
        String status, LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
