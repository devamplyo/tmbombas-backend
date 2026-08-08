package com.projeto.th_piscinas_api.dto.serviceOrder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PriceRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        String description   // optional: detail what's being quoted
) {
}
