package com.projeto.th_piscinas_api.dto.receivable;

import com.projeto.th_piscinas_api.util.PaymentMethod;
import com.projeto.th_piscinas_api.util.ReceivableSource;
import com.projeto.th_piscinas_api.util.ReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableResponse(
        Long id,
        ReceivableSource sourceType,
        Long sourceId,
        String clientName,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        ReceivableStatus status,
        PaymentMethod paymentMethod,
        LocalDate receivedDate
) {
}
