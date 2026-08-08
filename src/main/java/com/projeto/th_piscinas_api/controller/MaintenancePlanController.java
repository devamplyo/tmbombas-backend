package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.maintenance.MaintenancePlanRequest;
import com.projeto.th_piscinas_api.dto.maintenance.MaintenancePlanResponse;
import com.projeto.th_piscinas_api.dto.maintenance.RegisterMaintenanceRequest;
import com.projeto.th_piscinas_api.dto.maintenance.ReleaseMaintenanceRequest;
import com.projeto.th_piscinas_api.service.MaintenancePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/maintenance-plans")
@RequiredArgsConstructor
public class MaintenancePlanController {


    private final MaintenancePlanService maintenancePlanService;

    // Read access also granted to Internal Salesperson (the "Overdue maintenance"
    // badge on Clients screens); create/register/release remains ADM only.
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    @GetMapping
    public ResponseEntity<List<MaintenancePlanResponse>> listOfMaintenance() {

        List<MaintenancePlanResponse> maintenancePlanResponses =
                maintenancePlanService.listOfMaintenance();

        return ResponseEntity.status(HttpStatus.OK).body(maintenancePlanResponses);
    }

    /**
     * Control panel: due in X days or overdue. E.g.: ?days=15
     */
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    @GetMapping("/due")
    public ResponseEntity<List<MaintenancePlanResponse>> due(
            @RequestParam(required = false) Integer days) {

        List<MaintenancePlanResponse> maintenancePlanResponseList = maintenancePlanService.due(days);

        return ResponseEntity.status(HttpStatus.OK).body(maintenancePlanResponseList);
    }

    @PreAuthorize("hasRole('ADM_MASTER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MaintenancePlanResponse> createMaintenancePlan(@Valid @RequestBody
                                                                         MaintenancePlanRequest req) {
        MaintenancePlanResponse maintenancePlanResponse =
                maintenancePlanService.createMaintenancePlan(req);

        return ResponseEntity.status(HttpStatus.CREATED).body(maintenancePlanResponse);
    }

    /**
     * Marks the maintenance as done and recalculates the next one.
     */
    @PreAuthorize("hasRole('ADM_MASTER')")
    @PostMapping("/{id}/register")
    public ResponseEntity<MaintenancePlanResponse> registerMaintenance(@PathVariable Long id,
                                                                       @RequestBody(required = false)
                                                                       RegisterMaintenanceRequest req) {
        MaintenancePlanResponse maintenancePlanResponse =
                maintenancePlanService.registerMaintenance(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(maintenancePlanResponse);
    }

    @PreAuthorize("hasRole('ADM_MASTER')")
    @PostMapping("/{id}/release")
    public ResponseEntity<MaintenancePlanResponse> release(@PathVariable Long id,
                                           @Valid @RequestBody ReleaseMaintenanceRequest req) {

        MaintenancePlanResponse response = maintenancePlanService.release(id, req.technicianId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
