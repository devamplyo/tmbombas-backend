package com.projeto.th_piscinas_api.dto.serviceOrder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServiceOrderItemRequest(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal value
) {
}
