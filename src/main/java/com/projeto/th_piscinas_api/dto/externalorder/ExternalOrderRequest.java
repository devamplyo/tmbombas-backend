package com.projeto.th_piscinas_api.dto.externalorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExternalOrderRequest(
        String customerName,
        String notes,
        @NotEmpty(message = "O pedido precisa de ao menos um item")
        @Valid List<ExternalOrderItemRequest> items
) {
}
