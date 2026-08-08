package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialFlowResponse;
import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialLaunchRequest;
import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialLaunchResponse;
import com.projeto.th_piscinas_api.dto.financiallaunch.FlowPeriodResponse;
import com.projeto.th_piscinas_api.service.FinancialReportService;
import com.projeto.th_piscinas_api.util.Granularidade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class FinancialLaunchController {

    private final FinancialReportService financialReportService;

    @GetMapping("/flow")
    public ResponseEntity<FinancialFlowResponse> flowPeriod(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Granularidade granularidade) {

        FinancialFlowResponse flow = financialReportService.fluxoPorPeriodo(inicio, fim, granularidade);

        return ResponseEntity.status(HttpStatus.OK).body(flow);
    }

    @PostMapping
    public ResponseEntity<FinancialLaunchResponse> createFinancialLaunch(
            @Valid @RequestBody FinancialLaunchRequest req) {

        FinancialLaunchResponse created = financialReportService.createFinancialLaunch(req);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
