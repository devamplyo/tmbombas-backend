package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.receivable.ConfirmReceivableRequest;
import com.projeto.th_piscinas_api.dto.receivable.ReceivableResponse;
import com.projeto.th_piscinas_api.model.Receivable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReceivableMapper {

    ReceivableResponse toResponse(Receivable entity);

    Receivable toConfirmReceivableEntity(ConfirmReceivableRequest request);
}
