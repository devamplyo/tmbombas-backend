package com.projeto.th_piscinas_api.dto.externalorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExternalOrderItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}
