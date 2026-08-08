package com.projeto.th_piscinas_api.dto.servicetask;

import com.projeto.th_piscinas_api.util.ServiceTaskStatus;

import java.time.LocalDate;

/** Partial update: only non-null fields are applied. */
public record ServiceTaskUpdateRequest(
        ServiceTaskStatus status,
        Long technicianId,
        String description,
        LocalDate scheduledDate,
        String scheduledTime
) {
}
