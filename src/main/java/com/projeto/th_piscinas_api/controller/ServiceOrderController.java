package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderUpdateRequest;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.ServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/serviceorders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO','VENDEDOR_EXTERNO','TECNICO_CONDOMINIAL')")
    public ResponseEntity<List<ServiceOrderResponse>> listOfServicesOrders() {

        List<ServiceOrderResponse> serviceOrders = serviceOrderService.listOfServicesOrders();

        return ResponseEntity.status(HttpStatus.OK).body(serviceOrders);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO','VENDEDOR_EXTERNO','TECNICO_CONDOMINIAL')")
    public ResponseEntity<ServiceOrderResponse> createServiceOrder(@Valid @RequestBody ServiceOrderRequest req,
                                                                   @AuthenticationPrincipal User creator) {

        ServiceOrderResponse serviceOrder = serviceOrderService.createServiceOrder(req, creator);

        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOrder);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
    public ResponseEntity<ServiceOrderResponse> updateServiceOrder(@PathVariable Long id,
                                                                   @Valid @RequestBody ServiceOrderUpdateRequest req) {

        ServiceOrderResponse serviceOrder = serviceOrderService.updateServiceOrder(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(serviceOrder);
    }
}
