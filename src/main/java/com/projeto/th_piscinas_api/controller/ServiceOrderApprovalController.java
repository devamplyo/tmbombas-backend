package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.serviceOrder.PriceRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.RejectRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.ServiceOrderApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/service-orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class ServiceOrderApprovalController {


    private final ServiceOrderApprovalService approvalService;

    @PostMapping("/{id}/price")
    public ResponseEntity<ServiceOrderResponse> setPrice(@PathVariable Long id,
                                   @Valid @RequestBody PriceRequest req) {

        ServiceOrderResponse response = approvalService.price(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ServiceOrderResponse> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal User approver) {

        ServiceOrderResponse response = approvalService.approve(id, approver);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ServiceOrderResponse> reject(@PathVariable Long id,
                                       @Valid @RequestBody RejectRequest req) {

        ServiceOrderResponse response = approvalService.reject(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
