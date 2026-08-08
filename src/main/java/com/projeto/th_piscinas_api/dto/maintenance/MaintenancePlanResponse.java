package com.projeto.th_piscinas_api.dto.maintenance;

import java.time.LocalDate;

public record MaintenancePlanResponse(
        Long id,
        Long clientId,
        String clientName,
        String description,
        Integer frequencyDays,
        LocalDate lastMaintenanceDate,
        LocalDate nextMaintenanceDate,
        Boolean active,
        long daysUntilDue,   // negativo = vencido; 0 = hoje; positivo = dias restantes
        Boolean released,
        Long technicianId
) {
}
