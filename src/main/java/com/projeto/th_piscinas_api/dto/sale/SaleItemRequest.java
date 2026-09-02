package com.projeto.th_piscinas_api.dto.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(

        @JsonProperty("product_id")
        @NotNull Long productId,
        @NotNull @Min(value = 1, message = "Quantidade mínima é 1") Integer quantity,
        // achado F19: preço opcional pra honrar o valor já cotado (ex.: pedido
        // do Vendedor Externo) em vez de buscar o preço atual do produto de
        // novo. Nulo (caso do PDV) continua usando o preço vigente do catálogo.
        // teste de robustez: sem esta validação, um preço negativo entraria
        // direto na multiplicação do total (SaleService), criando venda com
        // total negativo e corrompendo o financeiro. @DecimalMin pula null
        // (o caso opcional do PDV segue válido); se vier, tem que ser > 0.
        @DecimalMin(value = "0.0", inclusive = false, message = "Preço unitário deve ser maior que zero")
        BigDecimal unitPrice
) {
}
