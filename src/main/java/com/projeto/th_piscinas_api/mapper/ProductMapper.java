package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.product.ProductRequest;
import com.projeto.th_piscinas_api.dto.product.ProductResponse;
import com.projeto.th_piscinas_api.dto.product.ProductSearchResponse;
import com.projeto.th_piscinas_api.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // create: id, timestamps and active are managed by the entity/DB, not the client
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    // update: mutates an existing managed entity, ignoring fields not owned by the request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);

    ProductSearchResponse toSearchResponse(Product p);
}
