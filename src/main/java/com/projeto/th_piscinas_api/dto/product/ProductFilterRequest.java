package com.projeto.th_piscinas_api.dto.product;

import java.util.Map;

public record ProductFilterRequest(
        Map<String, Object> query,
        String sort,
        Integer limit
) {
}
