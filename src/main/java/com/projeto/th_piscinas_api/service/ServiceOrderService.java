package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderUpdateRequest;
import com.projeto.th_piscinas_api.exception.ClientNotFoundException;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.exception.ProfileNotValidateException;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderItemRequest;
import com.projeto.th_piscinas_api.mapper.ServiceOrderMapper;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.ServiceOrderItem;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ClientRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceOrderMapper serviceOrderMapper;


    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> listOfServicesOrders() {

        List<ServiceOrder> order = serviceOrderRepository.findAllWithClientTechnicianAndItems();

        return order.stream().map(serviceOrderMapper::toResponse).toList();
    }

    @Transactional
    public ServiceOrderResponse createServiceOrder(ServiceOrderRequest req, User creator) {
        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado: " + req.clientId()));

        String orderNumber = String.format("OS-%d-%05d",
                LocalDate.now().getYear(),
                Math.abs(UUID.randomUUID().hashCode()) % 99999 + 1);

        ServiceOrder order = ServiceOrder.builder()
                .client(client)
                .title(req.title())
                .description(req.description())
                .orderNumber(orderNumber)
                .scheduledDate(req.scheduledDate())
                .price(req.price())
                .status(ServiceOrderStatus.ABERTA)
                .type(req.type() != null ? req.type() : ServiceOrderType.OS)
                .createdById(creator != null ? creator.getId() : null)
                .build();

        if (req.technicianId() != null) {
            order.setTechnician(searchTechnician(req.technicianId()));
        }

        if (req.items() != null && !req.items().isEmpty()) {
            List<ServiceOrderItem> items = buildItems(order, req.items());
            order.getItems().addAll(items);
            order.setPrice(sumItems(items));
        }

        ServiceOrder savedOrder = serviceOrderRepository.save(order);

        return serviceOrderMapper.toResponse(savedOrder);
    }

    private List<ServiceOrderItem> buildItems(ServiceOrder order, List<ServiceOrderItemRequest> requests) {
        List<ServiceOrderItem> items = new ArrayList<>();
        for (ServiceOrderItemRequest r : requests) {
            items.add(ServiceOrderItem.builder()
                    .serviceOrder(order)
                    .name(r.name())
                    .description(r.description())
                    .value(r.value())
                    .build());
        }
        return items;
    }

    private BigDecimal sumItems(List<ServiceOrderItem> items) {
        return items.stream()
                .map(ServiceOrderItem::getValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public ServiceOrderResponse updateServiceOrder(Long id, ServiceOrderUpdateRequest req) {
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Ordem de serviço não encontrada: " + id));

        if (req.description() != null) order.setDescription(req.description());
        if (req.scheduledDate() != null) order.setScheduledDate(req.scheduledDate());
        // A budget with items has its total defined by their sum — doesn't accept a standalone price,
        // otherwise the PDF would end up with items adding up to X and total Y.
        if (req.price() != null && order.getItems().isEmpty()) order.setPrice(req.price());
        if (req.technicianId() != null) order.setTechnician(searchTechnician(req.technicianId()));

        if (req.status() != null) {
            order.setStatus(req.status());
            // automatically marks the completion date
            if (req.status() == ServiceOrderStatus.CONCLUIDA && order.getCompletedAt() == null) {
                order.setCompletedAt(LocalDateTime.now());
            }
        }

        ServiceOrder savedOrder = serviceOrderRepository.save(order);

        return serviceOrderMapper.toResponse(savedOrder);
    }

    /**
     * Ensures the assigned user really is a technician.
     */
    private User searchTechnician(Long technicianId) {
        User user = userRepository.findById(technicianId)
                .orElseThrow(() -> new ClientNotFoundException(
                        "Usuário não encontrado: " + technicianId));

        if (user.getPerfil() != Perfil.TECNICO_CONDOMINIAL) {
            throw new ProfileNotValidateException("O usuário informado não é um técnico");
        }
        return user;
    }

}
