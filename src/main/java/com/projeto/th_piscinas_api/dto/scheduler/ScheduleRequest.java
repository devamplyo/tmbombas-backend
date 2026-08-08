package com.projeto.th_piscinas_api.dto.scheduler;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ScheduleRequest(
        @NotNull Long technicianId,
        @NotNull LocalDateTime scheduledDate,   // start
        @Positive Integer durationMinutes
) {
}
