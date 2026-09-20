package com.projeto.th_piscinas_api.dto.invoice;

import jakarta.validation.constraints.NotNull;

/**
 * Data to issue an NF-e from a sale. Value, items and payment come from the sale in the
 * database — never from the front. The recipient is either a registered client
 * ({@code clientId}) or filled in here (walk-in customer: document and address are required).
 */
public record NfeEmitRequest(
        @NotNull Long saleId,
        Long clientId,
        String name,
        String document,
        String stateRegistration,
        String street,
        String number,
        String district,
        String city,
        String state,
        String zipCode
) {
}
