package com.projeto.th_piscinas_api.dto.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projeto.th_piscinas_api.util.PaymentMethod;
import com.projeto.th_piscinas_api.util.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record SaleRequest(

        @JsonProperty("customer_name")
        String customerName,
        PaymentType paymentType,
        PaymentMethod paymentMethod,
        LocalDate dueDate,
        @NotEmpty(message = "A venda precisa de ao menos um item")
        @Valid List<SaleItemRequest> items
) {
}
