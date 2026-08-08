package com.projeto.th_piscinas_api.dto.serviceOrder;

import java.math.BigDecimal;

public record ServiceOrderItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal value
) {
}
