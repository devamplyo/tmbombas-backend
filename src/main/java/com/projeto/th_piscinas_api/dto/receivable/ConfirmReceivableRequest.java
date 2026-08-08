package com.projeto.th_piscinas_api.dto.receivable;

import com.projeto.th_piscinas_api.util.PaymentMethod;

import java.time.LocalDate;

public record ConfirmReceivableRequest(
        PaymentMethod paymentMethod,
        LocalDate receivedDate
) {
}
