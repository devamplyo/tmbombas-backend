package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderItemRequest;
import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderRequest;
import com.projeto.th_piscinas_api.dto.externalorder.ExternalOrderResponse;
import com.projeto.th_piscinas_api.dto.sale.SaleItemRequest;
import com.projeto.th_piscinas_api.dto.sale.SaleRequest;
import com.projeto.th_piscinas_api.dto.sale.SaleResponse;
import com.projeto.th_piscinas_api.exception.OrderInAlreadyInProgressException;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.exception.ProductNotFoundException;
import com.projeto.th_piscinas_api.mapper.ExternalOrderMapper;
import com.projeto.th_piscinas_api.model.ExternalOrder;
import com.projeto.th_piscinas_api.model.ExternalOrderItem;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ExternalOrderRepository;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalOrderService {

    private final ExternalOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;
    private final ExternalOrderMapper externalOrderMapper;

    /** External salesperson sends the order. Does NOT reduce stock — only records the request. */
    @Transactional
    public ExternalOrderResponse createOrder(ExternalOrderRequest req, User seller) {
        ExternalOrder order = ExternalOrder.builder()
                .sellerId(seller != null ? seller.getId() : null)
                .sellerName(seller != null ? seller.getNome() : null)
                .customerName(req.customerName())
                .notes(req.notes())
                .status(OrderStatus.ENVIADO)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (ExternalOrderItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Produto não encontrado: " + itemReq.productId()));

            BigDecimal unitPrice = product.getPrice();          // estimate
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(subtotal);

            order.addItem(ExternalOrderItem.builder()
                    .product(product).quantity(itemReq.quantity())
                    .unitPrice(unitPrice).subtotal(subtotal).build());
        }
        order.setTotal(total);
        return externalOrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<ExternalOrderResponse> listByStatus(OrderStatus status) {

        List<ExternalOrder> orders = (status != null)
                ? orderRepository.findByStatus(status)
                : orderRepository.findAll();

        return orders.stream().map(externalOrderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExternalOrderResponse> listMineOrder(User seller) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId())
                .stream().map(externalOrderMapper::toResponse).toList();
    }

    /** ADM approves: the order TURNS INTO a sale (only then does it reduce stock + financials). */
    @Transactional
    public ExternalOrderResponse approveOrder(Long id, User admin) {
        ExternalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Pedido não encontrado: " + id));

        demandSend(order);

        // builds the sale from the order's items, recorded under the original salesperson
        List<SaleItemRequest> saleItems = order.getItems().stream()
                .map(i -> new SaleItemRequest(i.getProduct().getId(), i.getQuantity()))
                .toList();
        SaleRequest saleReq = new SaleRequest(
                order.getCustomerName(), null, null, null, saleItems);

        User seller = order.getSellerId() != null
                ? userRepository.findById(order.getSellerId()).orElse(null) : null;

        // createSale validates stock NOW and throws 409 if insufficient -> rolls back the approval
        SaleResponse sale = saleService.createSale(saleReq, seller);

        order.setStatus(OrderStatus.APROVADO);
        order.setSaleId(sale.id());          // adjust to match the id field name in your SaleResponse
        order.setProcessedById(admin != null ? admin.getId() : null);
        order.setProcessedAt(LocalDateTime.now());

        return externalOrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public ExternalOrderResponse rejectOrder(Long id, User admin, String reason) {

        ExternalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pedido não encontrado: " + id));

        demandSend(order);
        order.setStatus(OrderStatus.REJEITADO);
        order.setRejectionReason(reason);
        order.setProcessedById(admin != null ? admin.getId() : null);
        order.setProcessedAt(LocalDateTime.now());
        return externalOrderMapper.toResponse(orderRepository.save(order));
    }

    private void demandSend(ExternalOrder order) {
        if (order.getStatus() != OrderStatus.ENVIADO) {
            throw new OrderInAlreadyInProgressException(
                    "Pedido já processado (status: " + order.getStatus() + ")");
        }
    }

}
