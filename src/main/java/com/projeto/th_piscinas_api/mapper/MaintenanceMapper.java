package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.maintenance.MaintenancePlanRequest;
import com.projeto.th_piscinas_api.dto.maintenance.MaintenancePlanResponse;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.MaintenancePlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MaintenanceMapper {


    @Mapping(target = "clientId",   source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "daysUntilDue",
            expression = "java(daysUntilDue(plan.getNextMaintenanceDate()))")
    MaintenancePlanResponse toResponse(MaintenancePlan plan);

    /** Helper used by the expression above. */
    default long daysUntilDue(LocalDate next) {
        return next == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), next);
    }

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "client",      source = "client")
    @Mapping(target = "active",      constant = "true")
    @Mapping(target = "nextMaintenanceDate",
            expression = "java(nextDate(request.lastMaintenanceDate(), request.frequencyDays()))")
    MaintenancePlan toEntity(MaintenancePlanRequest request, Client client);

    /** Next date: starts from the last maintenance (or today) + the frequency. */
    default LocalDate nextDate(LocalDate last, Integer frequencyDays) {
        LocalDate base = (last != null) ? last : LocalDate.now();
        return base.plusDays(frequencyDays);

    }
}
