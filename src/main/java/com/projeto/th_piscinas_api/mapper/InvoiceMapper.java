package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.nfse.NfseResponse;
import com.projeto.th_piscinas_api.model.Invoice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    NfseResponse toResponse(Invoice invoice);
}
