package com.projeto.th_piscinas_api.dto.collaborator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleHistoryItem(
        Long saleId, String customerName,
        BigDecimal total, LocalDateTime date
) {
}
