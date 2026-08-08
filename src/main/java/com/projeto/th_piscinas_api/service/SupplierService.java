package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.supplier.SupplierRequest;
import com.projeto.th_piscinas_api.dto.supplier.SupplierResponse;
import com.projeto.th_piscinas_api.exception.OrderInAlreadyInProgressException;
import com.projeto.th_piscinas_api.mapper.SupplierMapper;
import com.projeto.th_piscinas_api.model.Supplier;
import com.projeto.th_piscinas_api.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Transactional(readOnly = true)
    public List<SupplierResponse> listOfSuppliers() {

        List<Supplier> supplierList = supplierRepository.findAll();

        return supplierList.stream().map(supplierMapper::toResponse).toList();
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest req) {
        if (req.cnpj() != null && supplierRepository.existsByCnpj(req.cnpj())) {
            throw new OrderInAlreadyInProgressException(
                    "Já existe fornecedor com o CNPJ " + req.cnpj());
        }

        Supplier newSupplier = supplierMapper.toEntity(req);
        newSupplier.setActive(true);
        supplierRepository.save(newSupplier);
//        Supplier s = Supplier.builder()
//                .name(req.name()).cnpj(req.cnpj())
//                .email(req.email()).phone(req.phone())
//                .active(true).build();
        return supplierMapper.toResponse(newSupplier);
    }


}
