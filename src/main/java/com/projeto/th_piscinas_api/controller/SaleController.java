package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.sale.CancelSaleRequest;
import com.projeto.th_piscinas_api.dto.sale.CancelSaleResponse;
import com.projeto.th_piscinas_api.dto.sale.SaleRequest;
import com.projeto.th_piscinas_api.dto.sale.SaleResponse;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO','VENDEDOR_EXTERNO')")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<SaleResponse>> listOfSales() {

        List<SaleResponse> sales = saleService.listOfSales();

        return ResponseEntity.status(HttpStatus.OK).body(sales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> findById(@PathVariable Long id) {

        SaleResponse sale = saleService.findSaleById(id);

        return ResponseEntity.status(HttpStatus.OK).body(sale);
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest req,
                                               @AuthenticationPrincipal User seller) {

        SaleResponse sale = saleService.createSale(req, seller);

        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    public ResponseEntity<CancelSaleResponse> cancelSale(@PathVariable Long id,
                                                         @Valid @RequestBody CancelSaleRequest req) {

        CancelSaleResponse sale = saleService.cancelSale(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(sale);
    }
}
