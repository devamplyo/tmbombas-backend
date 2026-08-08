package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderRequest;
import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderResponse;
import com.projeto.th_piscinas_api.dto.serviceOrder.RejectRequest;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.ExternalOrderService;
import com.projeto.th_piscinas_api.util.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/external-orders")
@RequiredArgsConstructor
public class ExternalOrderController {

    private final ExternalOrderService externalOrderService;

    /**
     * External salesperson sends the order.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDEDOR_EXTERNO')")
    public ResponseEntity<ExternalOrderResponse> createOrder(@Valid @RequestBody ExternalOrderRequest req,
                                                             @AuthenticationPrincipal User seller) {
        ExternalOrderResponse response = externalOrderService.createOrder(req, seller);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * External salesperson sees their own orders.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('VENDEDOR_EXTERNO')")
    public ResponseEntity<List<ExternalOrderResponse>> mineOrder(@AuthenticationPrincipal User seller) {

        List<ExternalOrderResponse> response = externalOrderService.listMineOrder(seller);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * ADM: order queue (default: all; ?status=ENVIADO for pending ones).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<List<ExternalOrderResponse>> listOfOrders(@RequestParam(required = false)
                                                                OrderStatus status) {

        List<ExternalOrderResponse> response = externalOrderService.listByStatus(status);

        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<ExternalOrderResponse> approveOrder(@PathVariable Long id,
                                         @AuthenticationPrincipal User admin) {

        ExternalOrderResponse response = externalOrderService.approveOrder(id, admin);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<ExternalOrderResponse> rejectOrder(@PathVariable Long id,
                                        @AuthenticationPrincipal User admin,
                                        @RequestBody RejectRequest req) {

        ExternalOrderResponse response = externalOrderService.rejectOrder(id, admin, req.reason());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
