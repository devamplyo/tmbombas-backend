package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.serviceOrder.PriceRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.RejectRequest;
import com.projeto.th_piscinas_api.dto.serviceOrder.ServiceOrderResponse;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.mapper.ServiceOrderMapper;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceOrderApprovalService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderMapper serviceOrderMapper;

    @Transactional
    public ServiceOrderResponse price(Long id, PriceRequest req) {

        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Ordem de serviço não encontrada: " + id));

        // can set a price when ABERTA (open), or reprice when ORCADA/REPROVADA (quoted/rejected)
        setStatus(order, Set.of(ServiceOrderStatus.ABERTA,
                        ServiceOrderStatus.ORCADA, ServiceOrderStatus.REPROVADA),
                "precificada");

        // A budget with items already has its total defined by their sum — repricing it
        // externally would leave the document contradictory (items adding up to X, total Y).
        if (!order.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Orçamento com itens tem o total calculado pela soma dos itens; "
                            + "edite os itens em vez de informar um preço avulso");
        }

        order.setPrice(req.price());
        if (req.description() != null) order.setDescription(req.description());
        order.setStatus(ServiceOrderStatus.ORCADA);
        order.setRejectionReason(null); // clears the previous rejection, if repricing

        return serviceOrderMapper.toResponse(serviceOrderRepository.save(order));
    }

    /** Approve: only from ORCADA (quoted), and requires a price to be set. */
    @Transactional
    public ServiceOrderResponse approve(Long id, User approver) {

        ServiceOrder order =serviceOrderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Ordem de serviço não encontrada: " + id));
        setStatus(order, Set.of(ServiceOrderStatus.ORCADA), "aprovada");

        if (order.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "OS sem preço não pode ser aprovada");
        }

        order.setStatus(ServiceOrderStatus.APROVADA);
        order.setApprovedById(approver != null ? approver.getId() : null);
        order.setApprovedAt(LocalDateTime.now());

        return serviceOrderMapper.toResponse(serviceOrderRepository.save(order));
    }

    /** Reject: only from ORCADA (quoted), with a mandatory reason. */
    @Transactional
    public ServiceOrderResponse reject(Long id, RejectRequest req) {
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Ordem de serviço não encontrada: " + id));

        setStatus(order, Set.of(ServiceOrderStatus.ORCADA), "reprovada");

        order.setStatus(ServiceOrderStatus.REPROVADA);
        order.setRejectionReason(req.reason());

        return serviceOrderMapper.toResponse(serviceOrderRepository.save(order));
    }


    private void setStatus(ServiceOrder order, Set<ServiceOrderStatus> permitidos, String acao) {
        if (!permitidos.contains(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "OS no status " + order.getStatus() + " não pode ser " + acao);
        }
    }

}
