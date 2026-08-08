package com.projeto.th_piscinas_api.dto.address;

public record AddressDto(
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String zipCode
) {
}
