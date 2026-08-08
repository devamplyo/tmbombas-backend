package com.projeto.th_piscinas_api.dto.stored;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceRecordResponse(
        Long id, Long serviceOrderId, Long authorId, String note,
        LocalDateTime createdAt, List<ServiceRecordPhotoResponse> photos
) {
}
