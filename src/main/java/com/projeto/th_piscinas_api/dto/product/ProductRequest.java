package com.projeto.th_piscinas_api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projeto.th_piscinas_api.util.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
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
        @NotBlank String unit,
        // fiscal data (NF-e) — optional; only the ADM Master's values are kept (see ProductService)
        @Pattern(regexp = "^\\d{8}$", message = "NCM deve ter 8 dígitos") String ncm,
        @Pattern(regexp = "^\\d{4}$", message = "CFOP deve ter 4 dígitos") String cfop,
        @Min(0) @Max(8) Integer origin,
        @Pattern(regexp = "^\\d{3}$", message = "CSOSN deve ter 3 dígitos") String csosn
) {
}
