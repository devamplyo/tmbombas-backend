package com.projeto.th_piscinas_api.dto.maintenance;

import java.time.LocalDate;

public record NextMaintenanceResponse(
        Long planId,
        Long clientId,
        String clientName,
        String description,
        LocalDate nextMaintenanceDate,
        long daysUntilDue
) {
}
