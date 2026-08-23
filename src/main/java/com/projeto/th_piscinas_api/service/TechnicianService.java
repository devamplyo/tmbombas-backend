package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.serviceOrder.BudgetRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.dto.technician.ClientWithServicesResponse;
import com.projeto.th_piscinas_api.dto.technician.TechnicianActivityHistoryResponse;
import com.projeto.th_piscinas_api.dto.technician.TechnicianActivityItem;
import com.projeto.th_piscinas_api.exception.ClientNotFoundException;
import com.projeto.th_piscinas_api.exception.OrderInAlreadyInProgressException;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.exception.ProfileNotValidateException;
import com.projeto.th_piscinas_api.mapper.ServiceOrderMapper;
import com.projeto.th_piscinas_api.mapper.TechnicianMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TechnicianService {


    private final ServiceOrderRepository serviceOrderRepository;
    private final TechnicianMapper technicianMapper;
    private final ClientRepository clientRepository;
    private final ServiceOrderMapper serviceOrderMapper;
    private final ReceivableService receivableService;

    /**
     * The technician's own history (scoped to the logged-in user).
     */
    @Transactional(readOnly = true)
    public TechnicianActivityHistoryResponse myActivities(User technician,
                                                          LocalDate from,
                                                          LocalDate to,
                                                          ServiceOrderStatus status) {
        List<ServiceOrder> orders = serviceOrderRepository
                .findByTechnicianIdOrderByCreatedAtDesc(technician.getId());

        // optional filters (by creation date and/or status)
        Stream<ServiceOrder> stream = orders.stream();
        if (from != null) {
            stream = stream.filter(o -> o.getCreatedAt() != null
                    && !o.getCreatedAt().toLocalDate().isBefore(from));
        }
        if (to != null) {
            stream = stream.filter(o -> o.getCreatedAt() != null
                    && !o.getCreatedAt().toLocalDate().isAfter(to));
        }
        if (status != null) {
            stream = stream.filter(o -> o.getStatus() == status);
        }
        List<ServiceOrder> filtered = stream.toList();

        List<TechnicianActivityItem> items = filtered.stream().map(technicianMapper::toResponse).toList();

        long completed = count(filtered, ServiceOrderStatus.CONCLUIDA);
        long scheduled = count(filtered, ServiceOrderStatus.AGENDADA);
        long inProgress = count(filtered, ServiceOrderStatus.EM_ANDAMENTO);

        return new TechnicianActivityHistoryResponse(
                technician.getId(), technician.getNome(),
                filtered.size(), completed, scheduled, inProgress, items);
    }

    private long count(List<ServiceOrder> list, ServiceOrderStatus status) {
        return list.stream().filter(o -> o.getStatus() == status).count();
    }


    @Transactional(readOnly = true)
    public List<ClientWithServicesResponse> clientsWithServices() {

        // 1. all service orders grouped by client (ONE query, no N+1)
        Map<Long, List<ServiceOrder>> servicesByClient = serviceOrderRepository.findAllWithClient()
                .stream()
                .filter(so -> so.getClient() != null)
                .collect(Collectors.groupingBy(so -> so.getClient().getId()));

        // 2. all active clients, each with their services (empty list if none)
        return clientRepository.findByActiveTrue().stream()
                .map(c -> new ClientWithServicesResponse(
                        c.getId(), c.getName(), c.getPhone(),
                        technicianMapper.toAddress(c.getAddress()),
                        servicesByClient.getOrDefault(c.getId(), List.of()).stream()
                                .map(technicianMapper::toServiceItem).toList()))
                .toList();
    }

    @Transactional
    public ServiceOrderResponse createBudget(BudgetRequest req, User technician) {

        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ClientNotFoundException(
                        "Cliente não encontrado: " + req.clientId()));

        ServiceOrder order = ServiceOrder.builder()
                .client(client)
                .technician(technician)          // the logged-in technician themself
                .title(req.title())
                .description(req.description())
                .price(req.price())
                .status(ServiceOrderStatus.ORCADA)
                .build();

        return serviceOrderMapper.toResponse(serviceOrderRepository.save(order));
    }

    /**
     * Technician starts the service: stamps started_at and moves it to EM_ANDAMENTO (in progress).
     */
    @Transactional
    public ServiceOrderResponse startService(Long orderId, User technician) {

        ServiceOrder order = searchTechniciansOrder(orderId, technician);

        // only makes sense to start an approved or scheduled service order
        if (order.getStatus() != ServiceOrderStatus.APROVADA
                && order.getStatus() != ServiceOrderStatus.AGENDADA) {
            throw new OrderInAlreadyInProgressException(
                    "OS no status " + order.getStatus() + " não pode ser iniciada");
        }
        if (order.getStartedAt() != null) {
            throw new OrderInAlreadyInProgressException("Serviço já iniciado");
        }

        order.setStartedAt(LocalDateTime.now());
        order.setStatus(ServiceOrderStatus.EM_ANDAMENTO);
        return serviceOrderMapper.toResponse(serviceOrderRepository.save(order));
    }

    /**
     * Technician completes the service: stamps completed_at (actual end time).
     */
    @Transactional
    public ServiceOrderResponse finishService(Long orderId, User technician) {
        ServiceOrder order = searchTechniciansOrder(orderId, technician);

        if (order.getStatus() != ServiceOrderStatus.EM_ANDAMENTO) {
            throw new OrderInAlreadyInProgressException(
                    "Só é possível concluir um serviço em andamento (status: "
                            + order.getStatus() + ")");
        }

        order.setCompletedAt(LocalDateTime.now());
        order.setStatus(ServiceOrderStatus.CONCLUIDA);
        ServiceOrder saved = serviceOrderRepository.save(order);

        // Same rule as the admin path: completed order opens the receivable.
        receivableService.createForServiceOrder(saved);

        return serviceOrderMapper.toResponse(saved);
    }

    /**
     * Ensures the service order exists AND belongs to the logged-in technician.
     */
    private ServiceOrder searchTechniciansOrder(Long orderId, User technician) {

        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Ordem de serviço não encontrada: " + orderId));

        if (order.getTechnician() == null
                || !order.getTechnician().getId().equals(technician.getId())) {
            throw new ProfileNotValidateException(
                    "Este serviço não está atribuído a você");
        }
        return order;
    }


}
