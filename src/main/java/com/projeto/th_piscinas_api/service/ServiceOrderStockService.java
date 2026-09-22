package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.ServiceOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Deducts stock for a service order's material items when it's completed (mirrors SaleService). */
@Service
@RequiredArgsConstructor
public class ServiceOrderStockService {

    @Transactional
    public void deductForCompletion(ServiceOrder order) {
        for (ServiceOrderItem item : order.getItems()) {
            if (item.getProduct() == null || item.getQuantity() == null) continue;

            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Estoque insuficiente para '" + product.getName()
                                + "'. Disponível: " + product.getStock());
            }
            product.setStock(product.getStock() - item.getQuantity());
        }
    }
}
