package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialLaunchRequest;
import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialLaunchResponse;
import com.projeto.th_piscinas_api.model.FinancialLaunch;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FinancialLaunchMapper {

    FinancialLaunchResponse toResponse(FinancialLaunch entity);

    FinancialLaunch toEntity(FinancialLaunchRequest request);
}
