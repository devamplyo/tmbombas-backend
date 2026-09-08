package com.projeto.th_piscinas_api.dto.serviceOrder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull Long clientId,
        @NotNull String order_number,
        @NotBlank String title,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price
) {
}
