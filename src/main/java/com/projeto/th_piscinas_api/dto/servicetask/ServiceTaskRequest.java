package com.projeto.th_piscinas_api.dto.servicetask;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ServiceTaskRequest(
        Long serviceOrderId,
        @NotNull(message = "Informe o técnico") Long technicianId,
        Long clientId,
        String description,
        LocalDate scheduledDate,
        String scheduledTime
) {
}
