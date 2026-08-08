package com.projeto.th_piscinas_api.exception;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        int Status,
        String message,
        LocalDateTime timestamp
) {
}
