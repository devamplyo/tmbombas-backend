package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.address.AddressDto;
import com.projeto.th_piscinas_api.dto.technician.ClientServiceItem;
import com.projeto.th_piscinas_api.dto.technician.TechnicianActivityItem;
import com.projeto.th_piscinas_api.model.Address;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TechnicianMapper {


    @Mapping(target = "orderId",    source = "id")
    @Mapping(target = "clientName", source = "client.name")
    TechnicianActivityItem toResponse(ServiceOrder serviceOrder);

    @Mapping(target = "orderId", source = "id")
        // status: enum ServiceOrderStatus -> String (MapStruct uses .name() under the hood)
    ClientServiceItem toServiceItem(ServiceOrder serviceOrder);

    AddressDto toAddress(Address address);

}
