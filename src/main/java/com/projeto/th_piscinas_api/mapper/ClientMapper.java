package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.client.ClientRequest;
import com.projeto.th_piscinas_api.dto.client.ClientResponse;
import com.projeto.th_piscinas_api.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {


    ClientResponse toResponse(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntity(ClientRequest request);

    /** Updates the existing entity in-place (PUT), without changing id/dates/active. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ClientRequest request, @MappingTarget Client client);
}
