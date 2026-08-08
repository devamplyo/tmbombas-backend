package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.receivable.ConfirmReceivableRequest;
import com.projeto.th_piscinas_api.dto.receivable.ReceivableListResponse;
import com.projeto.th_piscinas_api.dto.receivable.ReceivableResponse;
import com.projeto.th_piscinas_api.service.ReceivableService;
import com.projeto.th_piscinas_api.util.ReceivableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/receivables")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping
    public ResponseEntity<ReceivableListResponse> listOfReceivables(@RequestParam(required = false)
                                                                    ReceivableStatus status) {
        ReceivableListResponse response = receivableService.listOfReceivables(status);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * POST /api/v1/admin/receivables/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReceivableResponse> confirmReceivable(@PathVariable Long id,
                                                                @RequestBody(required = false)
                                                                ConfirmReceivableRequest req) {

        ReceivableResponse response = receivableService.confirmReceivable(id, req);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
