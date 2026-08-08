package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.supplier.SupplierRequest;
import com.projeto.th_piscinas_api.dto.supplier.SupplierResponse;
import com.projeto.th_piscinas_api.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

    SupplierResponse toResponse(Supplier supplier);

    Supplier toEntity(SupplierRequest request);
}
