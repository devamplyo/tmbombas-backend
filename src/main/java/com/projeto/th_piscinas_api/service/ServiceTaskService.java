package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskRequest;
import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskResponse;
import com.projeto.th_piscinas_api.dto.servicetask.ServiceTaskUpdateRequest;
import com.projeto.th_piscinas_api.mapper.ServiceTaskMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.ServiceTask;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.repository.ServiceTaskRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.ServiceTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTaskService {

    private final ServiceTaskRepository serviceTaskRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceTaskMapper serviceTaskMapper;

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> listTasks(Long technicianId) {
        log.info("Listando tarefas de serviço - technicianId={}", technicianId);
        List<ServiceTask> tasks = (technicianId != null)
                ? serviceTaskRepository.findByTechnicianIdOrderByScheduledDateDesc(technicianId)
                : serviceTaskRepository.findAllByOrderByScheduledDateDesc();
        log.debug("Listando tarefas de serviço - {} encontrada(s)", tasks.size());
        return tasks.stream().map(serviceTaskMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceTaskResponse getTask(Long id) {
        log.info("Buscando tarefa de serviço - id={}", id);
        return serviceTaskMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ServiceTaskResponse createTask(ServiceTaskRequest req) {
        log.info("Criando tarefa de serviço - technicianId={} scheduledDate={}",
                req.technicianId(), req.scheduledDate());

        User technician = resolveTechnician(req.technicianId());

        ServiceTask task = ServiceTask.builder()
                .technician(technician)
                .description(req.description())
                .scheduledDate(req.scheduledDate())
                .scheduledTime(req.scheduledTime())
                .status(ServiceTaskStatus.AGENDADO)
                .build();

        if (req.clientId() != null) {
            task.setClient(clientRepository.findById(req.clientId())
                    .orElseThrow(() -> {
                        log.warn("Criação de tarefa rejeitada - cliente não encontrado: {}", req.clientId());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Cliente não encontrado: " + req.clientId());
                    }));
        }
        if (req.serviceOrderId() != null) {
            task.setServiceOrder(serviceOrderRepository.findById(req.serviceOrderId())
                    .orElseThrow(() -> {
                        log.warn("Criação de tarefa rejeitada - OS não encontrada: {}", req.serviceOrderId());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Ordem de serviço não encontrada: " + req.serviceOrderId());
                    }));
        }

        ServiceTask savedTask = serviceTaskRepository.save(task);
        log.info("Tarefa de serviço criada - id={} technician={}", savedTask.getId(), technician.getMatricula());
        return serviceTaskMapper.toResponse(savedTask);
    }

    @Transactional
    public ServiceTaskResponse updateTask(Long id, ServiceTaskUpdateRequest req) {
        log.info("Atualizando tarefa de serviço - id={} novoStatus={}", id, req.status());

        ServiceTask task = findOrThrow(id);

        if (req.description() != null)   task.setDescription(req.description());
        if (req.scheduledDate() != null) task.setScheduledDate(req.scheduledDate());
        if (req.scheduledTime() != null) task.setScheduledTime(req.scheduledTime());
        if (req.technicianId() != null)  task.setTechnician(resolveTechnician(req.technicianId()));

        if (req.status() != null) {
            log.debug("Tarefa {} mudando status {} -> {}", id, task.getStatus(), req.status());
            task.setStatus(req.status());
            if (req.status() == ServiceTaskStatus.EM_ANDAMENTO && task.getStartedAt() == null) {
                task.setStartedAt(LocalDateTime.now());
            }
            if (req.status() == ServiceTaskStatus.CONCLUIDO && task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }

        ServiceTask savedTask = serviceTaskRepository.save(task);
        log.info("Tarefa de serviço atualizada - id={} status={}", savedTask.getId(), savedTask.getStatus());
        return serviceTaskMapper.toResponse(savedTask);
    }

    private User resolveTechnician(Long technicianId) {
        User user = userRepository.findById(technicianId)
                .orElseThrow(() -> {
                    log.warn("Técnico não encontrado: id={}", technicianId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Usuário não encontrado: " + technicianId);
                });
        if (user.getPerfil() != Perfil.TECNICO_CONDOMINIAL) {
            log.warn("Usuário {} não é técnico (perfil={})", technicianId, user.getPerfil());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O usuário informado não é um técnico");
        }
        return user;
    }

    private ServiceTask findOrThrow(Long id) {
        return serviceTaskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tarefa de serviço não encontrada: id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada: " + id);
                });
    }
}
