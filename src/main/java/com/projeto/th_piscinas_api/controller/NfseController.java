package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.nfse.NfseCancelRequest;
import com.projeto.th_piscinas_api.dto.nfse.NfseEmitRequest;
import com.projeto.th_piscinas_api.dto.nfse.NfseResponse;
import com.projeto.th_piscinas_api.dto.nfse.NfseStatusResponse;
import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.service.NfseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * NFS-e (service invoice). The front consumes these same paths that used to
 * be served by the Node backend.
 */
@RestController
@RequestMapping("/api/nfse")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class NfseController {

    private final NfseService nfseService;

    @GetMapping("/status")
    public ResponseEntity<NfseStatusResponse> status() {
        return ResponseEntity.ok(nfseService.status());
    }

    @GetMapping
    public ResponseEntity<List<NfseResponse>> listByServiceOrder(
            @RequestParam("service_order_id") String serviceOrderId) {
        return ResponseEntity.ok(nfseService.listByServiceOrder(serviceOrderId));
    }

    @PostMapping("/emit")
    public ResponseEntity<NfseResponse> emit(@Valid @RequestBody NfseEmitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nfseService.emit(req));
    }

    @GetMapping("/{id}/consult")
    public ResponseEntity<NfseResponse> consult(@PathVariable Long id) {
        return ResponseEntity.ok(nfseService.consult(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<NfseResponse> cancel(@PathVariable Long id, @Valid @RequestBody NfseCancelRequest req) {
        return ResponseEntity.ok(nfseService.cancel(id, req.justificativa()));
    }

    /**
     * Dry-run: builds the exact payload that would be sent to Focus, without
     * calling Focus, without persisting anything and without consuming DPS
     * numbering. Useful to check the payload against an already-validated
     * issuance before issuing for real.
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> preview(@RequestParam("service_order_id") Long serviceOrderId) {
        return ResponseEntity.ok(nfseService.preview(serviceOrderId));
    }

    @GetMapping("/{id}/simulado")
    public ResponseEntity<String> simulado(@PathVariable Long id,
                                           @RequestParam(defaultValue = "html") String fmt) {
        Invoice inv = nfseService.getInvoiceForReceipt(id);
        if ("xml".equalsIgnoreCase(fmt)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(nfseService.simuladoXml(inv));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(nfseService.simuladoHtml(inv));
    }

    /**
     * Local handler: returns {@code {"error": "..."}} preserving the contract
     * that the front (NfseSection) already consumes, without touching GlobalExceptionHandler.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() == null ? "Erro ao processar NFS-e." : ex.getReason()));
    }
}
