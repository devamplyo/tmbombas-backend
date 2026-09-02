package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.stored.ServiceRecordResponse;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.service.ServiceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/technician/service-orders/{id}/records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','TECNICO_CONDOMINIAL')")
public class ServiceRecordController {

    private final ServiceRecordService serviceRecordService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceRecordResponse> addRecord(
            @PathVariable Long id,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @AuthenticationPrincipal User technician) throws IOException {

        ServiceRecordResponse record = serviceRecordService.addRecord(id, note, photos, technician);

        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @GetMapping
    public ResponseEntity<List<ServiceRecordResponse>> list(@PathVariable Long id,
                                                            @AuthenticationPrincipal User caller) {

        List<ServiceRecordResponse> response = serviceRecordService.listByOrder(id, caller);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
