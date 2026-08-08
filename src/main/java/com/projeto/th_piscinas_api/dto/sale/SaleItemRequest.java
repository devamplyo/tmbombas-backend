package com.projeto.th_piscinas_api.dto.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(

        @JsonProperty("product_id")
        @NotNull Long productId,
        @NotNull @Min(value = 1, message = "Quantidade mínima é 1") Integer quantity
) {
}
