package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.supplier.SupplierRequest;
import com.projeto.th_piscinas_api.dto.supplier.SupplierResponse;
import com.projeto.th_piscinas_api.dto.supplier.SupplierSpendingResponse;
import com.projeto.th_piscinas_api.service.SupplierReportService;
import com.projeto.th_piscinas_api.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierReportService supplierReportService;

    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    @GetMapping
    public ResponseEntity<List<SupplierResponse>> listOfSuppliers() {

        List<SupplierResponse> responseList = supplierService.listOfSuppliers();

        return ResponseEntity.status(HttpStatus.OK).body(responseList);
    }

    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest req) {

        SupplierResponse response = supplierService.createSupplier(req);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Spending-by-supplier chart (ADM Master). */
    @PreAuthorize("hasRole('ADM_MASTER')")
    @GetMapping("/spending")
    public ResponseEntity<SupplierSpendingResponse> spendingBySupplier(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        SupplierSpendingResponse response = supplierReportService.spentBySupplier(inicio, fim);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
