package com.projeto.th_piscinas_api.dto.maintenance;

import jakarta.validation.constraints.NotNull;

public record ReleaseMaintenanceRequest(
        @NotNull Long technicianId
) {
}
