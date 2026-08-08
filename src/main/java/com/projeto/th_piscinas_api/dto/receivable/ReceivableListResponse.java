package com.projeto.th_piscinas_api.dto.receivable;

import java.math.BigDecimal;
import java.util.List;

public record ReceivableListResponse(
        List<ReceivableResponse> receivables,
        BigDecimal total
) {
}
