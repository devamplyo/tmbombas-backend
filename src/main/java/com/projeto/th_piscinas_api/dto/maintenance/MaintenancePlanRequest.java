package com.projeto.th_piscinas_api.dto.maintenance;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MaintenancePlanRequest(

        @JsonProperty("client_id")
        @NotNull Long clientId,
        String description,
        @NotNull Long technicianId,
        @JsonProperty("frequency_days")
        @NotNull @Min(1) Integer frequencyDays,
        LocalDate lastMaintenanceDate   // optional: when the last one was (defines the next one)
) {
}
