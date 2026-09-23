package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.sale.CancelSaleResponse;
import com.projeto.th_piscinas_api.dto.sale.SaleItemResponse;
import com.projeto.th_piscinas_api.dto.sale.SaleResponse;
import com.projeto.th_piscinas_api.model.Sale;
import com.projeto.th_piscinas_api.model.SaleItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SaleMapper {

    @Mapping(target = "clientId",   source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    SaleResponse toSaleResponse(Sale sale);

    @Mapping(target = "productId", source = "product.id")
    SaleItemResponse toItemResponse(SaleItem item);

    CancelSaleResponse toCancelSaleResponse(Sale sale);
}
