package com.projeto.th_piscinas_api.service;


import com.projeto.th_piscinas_api.dto.maintenance.MaintenancePlanRequest;
import com.projeto.th_piscinas_api.dto.maintenance.MaintenancePlanResponse;
import com.projeto.th_piscinas_api.dto.maintenance.NextMaintenanceResponse;
import com.projeto.th_piscinas_api.dto.maintenance.RegisterMaintenanceRequest;
import com.projeto.th_piscinas_api.exception.ClientNotFoundException;
import com.projeto.th_piscinas_api.exception.MaintenancePlanNotFoundException;
import com.projeto.th_piscinas_api.exception.ProfileNotValidateException;
import com.projeto.th_piscinas_api.mapper.MaintenanceMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.MaintenancePlan;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.MaintenancePlanRepository;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenancePlanService {

    private final MaintenancePlanRepository maintenancePlanRepository;
    private final ClientRepository clientRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<MaintenancePlanResponse> listOfMaintenance() {

      List<MaintenancePlan> maintenancePlanList = maintenancePlanRepository.findAll();

        return maintenancePlanList.stream().map(maintenanceMapper::toResponse).toList();
    }

    /** The control panel: who's due within X days (default 7) or already overdue. */
    @Transactional(readOnly = true)
    public List<MaintenancePlanResponse> due(Integer days) {
        int janela = (days != null) ? days : 7;
        LocalDate limite = LocalDate.now().plusDays(janela);
        return maintenancePlanRepository
                .findByActiveTrueAndNextMaintenanceDateLessThanEqualOrderByNextMaintenanceDateAsc(limite)
                .stream().map(maintenanceMapper::toResponse).toList();
    }

    @Transactional
    public MaintenancePlanResponse createMaintenancePlan(MaintenancePlanRequest req) {
        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ClientNotFoundException(
                        "Cliente não encontrado: " + req.clientId()));

        MaintenancePlan plan = maintenanceMapper.toEntity(req, client);

        maintenancePlanRepository.save(plan);

        return maintenanceMapper.toResponse(plan);
    }

    /** Records that the maintenance was performed and pushes the cycle forward. */
    @Transactional
    public MaintenancePlanResponse registerMaintenance(Long id, RegisterMaintenanceRequest req) {
        MaintenancePlan plan = maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plano de manutenção não encontrado: " + id));

        LocalDate data = (req != null && req.date() != null) ? req.date() : LocalDate.now();

        plan.setLastMaintenanceDate(data);
        plan.setNextMaintenanceDate(data.plusDays(plan.getFrequencyDays()));

        maintenancePlanRepository.save(plan);

        return maintenanceMapper.toResponse(plan);
    }

    @Transactional
    public MaintenancePlanResponse release(Long id, Long technicianId) {

        MaintenancePlan plan = maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new MaintenancePlanNotFoundException(
                        "Plano de manutenção não encontrado: " + id));

        User tech = userRepository.findById(technicianId)
                .orElseThrow(() -> new ClientNotFoundException(
                        "Usuário não encontrado: " + technicianId));

        if (tech.getPerfil() != Perfil.TECNICO_CONDOMINIAL) {
            throw new ProfileNotValidateException(
                    "O usuário informado não é um técnico");
        }

        plan.setReleased(true);
        plan.setTechnicianId(technicianId);

        return maintenanceMapper.toResponse(maintenancePlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<NextMaintenanceResponse> nextForTechnician(User technician) {
        return maintenancePlanRepository
                .findByReleasedTrueAndActiveTrueAndTechnicianIdOrderByNextMaintenanceDateAsc
                        (technician.getId())
                .stream()
                .map(p -> new NextMaintenanceResponse(
                        p.getId(),
                        p.getClient() != null ? p.getClient().getId() : null,
                        p.getClient() != null ? p.getClient().getName() : null,
                        p.getDescription(),
                        p.getNextMaintenanceDate(),
                        java.time.temporal.ChronoUnit.DAYS.between(
                                LocalDate.now(), p.getNextMaintenanceDate())))
                .toList();
    }



}
