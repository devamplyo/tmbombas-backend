package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceOrderMapper {

    @Mapping(target = "clientId",       source = "client.id")
    @Mapping(target = "clientName",     source = "client.name")
    @Mapping(target = "technicianId",   source = "technician.id")
    @Mapping(target = "technicianName", source = "technician.nome") // User usa 'nome'
    ServiceOrderResponse toResponse(ServiceOrder order);

    
}
