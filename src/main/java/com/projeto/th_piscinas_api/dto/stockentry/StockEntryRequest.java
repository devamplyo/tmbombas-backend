package com.projeto.th_piscinas_api.dto.stockentry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record StockEntryRequest(
        @NotNull Long supplierId,
        String documentNumber,
        LocalDate entryDate,        // opcional: default hoje
        @NotEmpty(message = "A entrada precisa de ao menos um item")
        @Valid List<StockEntryItemRequest> items
) {
}
