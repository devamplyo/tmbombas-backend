package com.projeto.th_piscinas_api.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        BigDecimal total,
        int totalQuantity,
        boolean valid
) {
}
