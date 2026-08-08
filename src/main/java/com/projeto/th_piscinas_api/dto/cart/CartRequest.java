package com.projeto.th_piscinas_api.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CartRequest(
        @NotEmpty(message = "O carrinho precisa de ao menos um item")
        @Valid List<CartItemRequest> items
) {
}
