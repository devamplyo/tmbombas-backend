package com.projeto.th_piscinas_api.controller;


import com.projeto.th_piscinas_api.dto.scheduler.AgendaItemResponse;
import com.projeto.th_piscinas_api.dto.scheduler.ScheduleRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADM_MASTER')")
public class SchedulingController {

    private final SchedulingService schedulingService;

    /** Schedules a service for a technician (assigns + sets time + status AGENDADA). */
    @PostMapping("/service-orders/{id}/schedule")
    public ServiceOrderResponse schedule(@PathVariable Long id,
                                         @Valid @RequestBody ScheduleRequest req) {
        return schedulingService.schedule(id, req);
    }

    /** Technicians' schedule. No filters = next 7 days, all technicians. */
    @GetMapping("/agenda")
    public List<AgendaItemResponse> agenda(
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return schedulingService.agenda(technicianId, from, to);
    }
}

