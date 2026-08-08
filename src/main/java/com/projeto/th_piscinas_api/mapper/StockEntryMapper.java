package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.stockentry.StockEntryItemResponse;
import com.projeto.th_piscinas_api.dto.stockentry.StockEntryResponse;
import com.projeto.th_piscinas_api.model.StockEntry;
import com.projeto.th_piscinas_api.model.StockEntryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockEntryMapper {

    @Mapping(target = "supplierId",   source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    StockEntryResponse toResponse(StockEntry entry);

    @Mapping(target = "productId",   source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    StockEntryItemResponse toItemResponse(StockEntryItem item);
}
