package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.externalSales.ExternalSalesReportResponse;
import com.projeto.th_piscinas_api.service.ExternalSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/external-sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class ExternalSalesController {

    private final ExternalSalesService externalSalesService;

    @GetMapping
    public ResponseEntity<ExternalSalesReportResponse> externalSalesReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        ExternalSalesReportResponse report = externalSalesService.externalSalesReport(inicio, fim);

        return ResponseEntity.status(HttpStatus.OK).body(report);
    }
}
