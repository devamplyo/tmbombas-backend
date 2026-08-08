package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskRequest;
import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskResponse;
import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskUpdateRequest;
import com.projeto.th_piscinas_api.service.ServiceTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicetasks")
@RequiredArgsConstructor
public class ServiceTaskController {

    private final ServiceTaskService serviceTaskService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
    public ResponseEntity<List<ServiceTaskResponse>> list(
            @RequestParam(required = false) Long technicianId) {
        return ResponseEntity.ok(serviceTaskService.listTasks(technicianId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
    public ResponseEntity<ServiceTaskResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(serviceTaskService.getTask(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
    public ResponseEntity<ServiceTaskResponse> create(@Valid @RequestBody ServiceTaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceTaskService.createTask(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
    public ResponseEntity<ServiceTaskResponse> update(@PathVariable Long id,
                                                      @RequestBody ServiceTaskUpdateRequest req) {
        return ResponseEntity.ok(serviceTaskService.updateTask(id, req));
    }
}
