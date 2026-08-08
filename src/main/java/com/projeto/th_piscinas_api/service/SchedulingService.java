package com.projeto.th_piscinas_api.service;


import com.projeto.th_piscinas_api.dto.scheduler.AgendaItemResponse;
import com.projeto.th_piscinas_api.dto.scheduler.ScheduleRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.exception.*;
import com.projeto.th_piscinas_api.mapper.ServiceOrderMapper;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final ServiceOrderMapper serviceOrderMapper;

    @Transactional
    public ServiceOrderResponse schedule(Long orderId, ScheduleRequest req) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Ordem de serviço não encontrada: " + orderId));

        User tech = userRepository.findById(req.technicianId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário não encontrado: " + req.technicianId()));
        if (tech.getPerfil() != Perfil.TECNICO_CONDOMINIAL) {
            throw new ProfileNotValidateException(
                    "O usuário informado não é um técnico");
        }

        int duration = req.durationMinutes() != null ? req.durationMinutes() : 60;
        LocalDateTime start = req.scheduledDate();
        LocalDateTime end = start.plusMinutes(duration);

        // prevents scheduling the same technician in an overlapping time slot
        List<ServiceOrder> conflitos = serviceOrderRepository.findConflicts(
                req.technicianId(), orderId,
                List.of(ServiceOrderStatus.AGENDADA, ServiceOrderStatus.EM_ANDAMENTO),
                start, end);
        if (!conflitos.isEmpty()) {
            ServiceOrder c = conflitos.get(0);
            throw new OrderInAlreadyInProgressException(
                    "Técnico já tem a OS #" + c.getId() + " agendada das "
                            + c.getScheduledDate() + " às " + c.getScheduledEnd());
        }

        order.setTechnician(tech);
        order.setScheduledDate(start);
        order.setScheduledEnd(end);
        order.setStatus(ServiceOrderStatus.AGENDADA);

        return serviceOrderMapper.toResponse(serviceOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<AgendaItemResponse> agenda(Long technicianId, LocalDateTime from, LocalDateTime to) {
        if (from == null) from = LocalDate.now().atStartOfDay();
        if (to == null)   to = from.plusDays(7);
        if (from.isAfter(to)) {
            throw new InvalidDateException(
                    "Data inicial não pode ser maior que a final");
        }

        List<ServiceOrder> orders = (technicianId != null)
                ? serviceOrderRepository
                .findByTechnicianIdAndScheduledDateBetweenOrderByScheduledDateAsc(technicianId, from, to)
                : serviceOrderRepository
                .findByScheduledDateBetweenOrderByScheduledDateAsc(from, to);

        return orders.stream().map(this::toAgendaItem).toList();
    }

    private AgendaItemResponse toAgendaItem(ServiceOrder so) {
        return new AgendaItemResponse(
                so.getId(), so.getTitle(),
                so.getClient() != null ? so.getClient().getName() : null,
                so.getTechnician() != null ? so.getTechnician().getId() : null,
                so.getTechnician() != null ? so.getTechnician().getNome() : null,
                so.getScheduledDate(), so.getScheduledEnd(),
                so.getStatus().name());
    }
}
