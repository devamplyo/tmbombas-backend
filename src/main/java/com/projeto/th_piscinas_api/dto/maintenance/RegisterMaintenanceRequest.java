package com.projeto.th_piscinas_api.dto.maintenance;

import java.time.LocalDate;

public record RegisterMaintenanceRequest(
        LocalDate date
) {
}
