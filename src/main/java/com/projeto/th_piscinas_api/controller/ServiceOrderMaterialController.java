package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderMaterialRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderMaterialResponse;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.ServiceOrderMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technician/service-orders/{id}/materials")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
public class ServiceOrderMaterialController {

    private final ServiceOrderMaterialService materialService;

    @GetMapping
    public ResponseEntity<List<ServiceOrderMaterialResponse>> list(@PathVariable Long id,
                                                                   @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(materialService.list(id, caller));
    }

    @PostMapping
    public ResponseEntity<ServiceOrderMaterialResponse> add(@PathVariable Long id,
                                                            @Valid @RequestBody ServiceOrderMaterialRequest req,
                                                            @AuthenticationPrincipal User technician) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.add(id, req, technician));
    }

    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> remove(@PathVariable Long id,
                                       @PathVariable Long materialId,
                                       @AuthenticationPrincipal User technician) {
        materialService.remove(id, materialId, technician);
        return ResponseEntity.noContent().build();
    }
}
