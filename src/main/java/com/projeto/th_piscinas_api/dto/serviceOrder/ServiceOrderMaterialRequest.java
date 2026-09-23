package com.projeto.th_piscinas_api.dto.serviceOrder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ServiceOrderMaterialRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}
