package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderItemResponse;
import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderResponse;
import com.projeto.th_piscinas_api.model.ExternalOrder;
import com.projeto.th_piscinas_api.model.ExternalOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExternalOrderMapper {

    ExternalOrderResponse toResponse(ExternalOrder order);

    @Mapping(target = "productId",   source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    ExternalOrderItemResponse toItemResponse(ExternalOrderItem item);
}
