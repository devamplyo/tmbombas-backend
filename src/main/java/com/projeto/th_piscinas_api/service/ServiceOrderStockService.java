package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.ServiceOrderItem;
import com.projeto.th_piscinas_api.model.ServiceOrderMaterial;
import com.projeto.th_piscinas_api.repository.ServiceOrderMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Deducts stock when a service order is completed (mirrors SaleService). */
@Service
@RequiredArgsConstructor
public class ServiceOrderStockService {

    private final ServiceOrderMaterialRepository materialRepository;

    /** Material the technician reported using wins; without it, falls back to the items planned in the budget. */
    @Transactional
    public void deductForCompletion(ServiceOrder order) {
        List<ServiceOrderMaterial> used = materialRepository.findByServiceOrderIdOrderByCreatedAtAsc(order.getId());
        if (!used.isEmpty()) {
            for (ServiceOrderMaterial material : used) {
                deduct(material.getProduct(), material.getQuantity());
            }
            return;
        }

        for (ServiceOrderItem item : order.getItems()) {
            if (item.getProduct() == null || item.getQuantity() == null) continue;
            deduct(item.getProduct(), item.getQuantity());
        }
    }

    private void deduct(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Estoque insuficiente para '" + product.getName()
                            + "'. Disponível: " + product.getStock());
        }
        product.setStock(product.getStock() - quantity);
    }
}
