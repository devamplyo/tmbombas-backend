package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.collaborator.CollaboratorDetailResponse;
import com.projeto.th_piscinas_api.dto.collaborator.CollaboratorSummaryResponse;
import com.projeto.th_piscinas_api.service.CollaboratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/collaborators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class CollaboratorController {


    private final CollaboratorService collaboratorService;

    /** List of collaborators with status + activity summary. */
    @GetMapping
    public ResponseEntity<List<CollaboratorSummaryResponse>> listOfSummary() {

        List<CollaboratorSummaryResponse> collaborators = collaboratorService.listOfSummary();

        return ResponseEntity.status(HttpStatus.OK).body(collaborators);
    }

    /** Detail of a collaborator with the full history (sales and service orders). */
    @GetMapping("/{id}")
    public ResponseEntity<CollaboratorDetailResponse> detailCollaborator(@PathVariable Long id) {

        CollaboratorDetailResponse collaborator = collaboratorService.detail(id);

        return ResponseEntity.status(HttpStatus.OK).body(collaborator);
    }
}
