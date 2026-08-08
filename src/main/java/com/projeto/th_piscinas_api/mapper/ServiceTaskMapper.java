package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskResponse;
import com.projeto.th_piscinas_api.model.ServiceTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceTaskMapper {

    @Mapping(target = "serviceOrderId", source = "serviceOrder.id")
    @Mapping(target = "technicianId",   source = "technician.id")
    @Mapping(target = "technicianName", source = "technician.nome")
    @Mapping(target = "clientId",       source = "client.id")
    @Mapping(target = "clientName",     source = "client.name")
    ServiceTaskResponse toResponse(ServiceTask task);
}
