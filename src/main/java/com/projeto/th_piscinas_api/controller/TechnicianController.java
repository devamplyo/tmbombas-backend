package com.projeto.th_piscinas_api.controller;


import com.projeto.th_piscinas_api.dto.maintenance.NextMaintenanceResponse;
import com.projeto.th_piscinas_api.dto.serviceOrder.BudgetRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.dto.technician.ClientWithServicesResponse;
import com.projeto.th_piscinas_api.dto.technician.TechnicianActivityHistoryResponse;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.MaintenancePlanService;
import com.projeto.th_piscinas_api.service.TechnicianService;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/technician")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
public class TechnicianController {


    private final TechnicianService technicianService;
    private final MaintenancePlanService maintenancePlanService;

    @GetMapping("/activities")
    public ResponseEntity<TechnicianActivityHistoryResponse> myActivities(
            @AuthenticationPrincipal User technician,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ServiceOrderStatus status) {

        TechnicianActivityHistoryResponse response = technicianService.
                myActivities(technician, from, to, status);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/next-maintenance")
    public ResponseEntity<List<NextMaintenanceResponse>> nextMaintenance(
            @AuthenticationPrincipal User technician) {

        List<NextMaintenanceResponse> response = maintenancePlanService.nextForTechnician(technician);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientWithServicesResponse>> clientsWithServices() {

        List<ClientWithServicesResponse> response = technicianService.clientsWithServices();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/budgets")
    public ResponseEntity<ServiceOrderResponse> createBudget(@Valid @RequestBody BudgetRequest req,
                                                             @AuthenticationPrincipal User technician) {
        ServiceOrderResponse response = technicianService.createBudget(req, technician);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/service-orders/{id}/start")
    public ResponseEntity<ServiceOrderResponse> start(@PathVariable Long id,
                                                      @AuthenticationPrincipal User technician) {

        ServiceOrderResponse response = technicianService.startService(id, technician);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/service-orders/{id}/finish")
    public ResponseEntity<ServiceOrderResponse> finish(@PathVariable Long id,
                                                       @AuthenticationPrincipal User technician) {

        ServiceOrderResponse response = technicianService.finishService(id, technician);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
