package com.projeto.th_piscinas_api.dto.technician;

import com.projeto.th_piscinas_api.dto.address.AddressDto;

import java.util.List;

public record ClientWithServicesResponse(
        Long clientId,
        String name,
        String phone,
        AddressDto address,
        List<ClientServiceItem> services
) {
}
