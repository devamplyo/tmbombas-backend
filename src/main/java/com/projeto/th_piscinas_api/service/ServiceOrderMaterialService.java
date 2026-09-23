package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderMaterialRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderMaterialResponse;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.exception.ProfileNotValidateException;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.ServiceOrderMaterial;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderMaterialRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.repository.ServiceTaskRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceOrderMaterialService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderMaterialRepository materialRepository;
    private final ProductRepository productRepository;
    private final ServiceTaskRepository serviceTaskRepository;

    @Transactional(readOnly = true)
    public List<ServiceOrderMaterialResponse> list(Long orderId, User caller) {
        ServiceOrder order = findOrder(orderId);
        if (caller.getPerfil() == Perfil.TECNICO_CONDOMINIAL) {
            requireAssigned(order, caller);
        }
        return materialRepository.findByServiceOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ServiceOrderMaterialResponse add(Long orderId, ServiceOrderMaterialRequest req, User technician) {
        ServiceOrder order = findOrder(orderId);
        requireAssigned(order, technician);
        requireOpen(order);

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + req.productId()));

        ServiceOrderMaterial material = ServiceOrderMaterial.builder()
                .serviceOrder(order)
                .product(product)
                .productName(product.getName())
                .quantity(req.quantity())
                .unitPrice(product.getPrice())
                .createdById(technician.getId())
                .build();

        return toResponse(materialRepository.save(material));
    }

    @Transactional
    public void remove(Long orderId, Long materialId, User technician) {
        ServiceOrder order = findOrder(orderId);
        requireAssigned(order, technician);
        requireOpen(order);

        ServiceOrderMaterial material = materialRepository.findById(materialId)
                .filter(m -> m.getServiceOrder().getId().equals(orderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Material não encontrado: " + materialId));
        materialRepository.delete(material);
    }

    private ServiceOrder findOrder(Long orderId) {
        return serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Ordem de serviço não encontrada: " + orderId));
    }

    // the OS's own technician, or the one a task linked to this OS was delegated to
    private void requireAssigned(ServiceOrder order, User user) {
        boolean isOrderTechnician = order.getTechnician() != null
                && order.getTechnician().getId().equals(user.getId());
        if (!isOrderTechnician
                && !serviceTaskRepository.existsByServiceOrderIdAndTechnicianId(order.getId(), user.getId())) {
            throw new ProfileNotValidateException("Este serviço não está atribuído a você");
        }
    }

    // stock is deducted when the OS is completed, so the material can't change afterwards
    private void requireOpen(ServiceOrder order) {
        if (order.getStatus() == ServiceOrderStatus.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "OS já concluída: o material não pode mais ser alterado");
        }
    }

    private ServiceOrderMaterialResponse toResponse(ServiceOrderMaterial m) {
        BigDecimal subtotal = m.getUnitPrice().multiply(BigDecimal.valueOf(m.getQuantity()));
        return new ServiceOrderMaterialResponse(m.getId(), m.getProduct().getId(),
                m.getProductName(), m.getQuantity(), m.getUnitPrice(), subtotal);
    }
}
