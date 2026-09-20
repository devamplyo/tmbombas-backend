package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.invoice.InvoiceResponse;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceStatusResponse;
import com.projeto.th_piscinas_api.dto.invoice.NfeEmitRequest;
import com.projeto.th_piscinas_api.dto.invoice.PendingInvoiceResponse;
import com.projeto.th_piscinas_api.dto.nfse.NfseCancelRequest;
import com.projeto.th_piscinas_api.dto.nfse.NfseEmitRequest;
import com.projeto.th_piscinas_api.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * "Notas Fiscais" screen: NF-e (venda) and NFS-e (serviço) together. ADM Master only.
 * Errors come back as {@code {"error": "..."}} — the same contract the NFS-e screen already used.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/status")
    public ResponseEntity<InvoiceStatusResponse> status() {
        return ResponseEntity.ok(invoiceService.status());
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> list() {
        return ResponseEntity.ok(invoiceService.list());
    }

    /** Sales and completed service orders that still have no note. */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingInvoiceResponse>> pending() {
        return ResponseEntity.ok(invoiceService.pending());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.get(id));
    }

    @PostMapping("/nfe")
    public ResponseEntity<InvoiceResponse> emitNfe(@Valid @RequestBody NfeEmitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.emitNfe(req));
    }

    @PostMapping("/nfse")
    public ResponseEntity<InvoiceResponse> emitNfse(@Valid @RequestBody NfseEmitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.emitNfse(req));
    }

    @PostMapping("/{id}/consult")
    public ResponseEntity<InvoiceResponse> consult(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.consult(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable Long id, @Valid @RequestBody NfseCancelRequest req) {
        return ResponseEntity.ok(invoiceService.cancel(id, req.justificativa()));
    }

    /** DANFE (pdf) or XML of an NF-e, streamed by the server. */
    @GetMapping("/{id}/documento")
    public ResponseEntity<byte[]> document(@PathVariable Long id, @RequestParam(defaultValue = "pdf") String fmt) {
        InvoiceService.DocumentFile file = invoiceService.document(id, fmt);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.filename()).build().toString())
                .body(file.body());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() == null ? "Erro ao processar a nota fiscal." : ex.getReason()));
    }
}
