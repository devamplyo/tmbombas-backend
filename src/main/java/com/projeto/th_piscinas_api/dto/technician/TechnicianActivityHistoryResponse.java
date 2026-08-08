package com.projeto.th_piscinas_api.dto.technician;

import java.util.List;

public record TechnicianActivityHistoryResponse(
        Long technicianId,
        String technicianName,
        long total,
        long completed,
        long scheduled,
        long inProgress,
        List<TechnicianActivityItem> activities
) {
}
