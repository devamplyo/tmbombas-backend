package com.projeto.th_piscinas_api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projeto.th_piscinas_api.util.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String code,
        @NotBlank String barcode,
        @NotBlank String manufacturer,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @JsonProperty("min_stock")
        @NotNull @Min(0) Integer minStock,
        @NotNull ProductCategory category,
        @JsonProperty("power_hp")
        Double powerHp,
        @JsonProperty("max_flow_rate")
        Double maxFlowRate,
        Integer voltage,
        @JsonProperty("unit")
        @NotBlank String unit
) {
}
