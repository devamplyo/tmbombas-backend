package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.cart.CartItemRequest;
import com.projeto.th_piscinas_api.dto.cart.CartItemResponse;
import com.projeto.th_piscinas_api.dto.cart.CartRequest;
import com.projeto.th_piscinas_api.dto.cart.CartResponse;
import com.projeto.th_piscinas_api.exception.ProductDisableException;
import com.projeto.th_piscinas_api.exception.ProductNotFoundException;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {


    private final ProductRepository productRepository;


    @Transactional(readOnly = true)
    public CartResponse calculateProductToCart(CartRequest req) {
        // merges quantities of the same product (if scanned twice, sums them up)
        Map<Long, Integer> qtyByProduct = new LinkedHashMap<>();

        for (CartItemRequest item : req.items()) {
            qtyByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int totalQuantity = 0;
        boolean valid = true;

        for (Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {

            Product p = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Produto não encontrado: " + entry.getKey()));

            if (!Boolean.TRUE.equals(p.getActive())) {
                throw new ProductDisableException(
                        "Produto inativo não pode ser vendido: " + p.getName());
            }

            int qty = entry.getValue();
            BigDecimal unitPrice = p.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            boolean stockOk = p.getStock() >= qty;
            if (!stockOk) valid = false;

            total = total.add(subtotal);
            totalQuantity += qty;

            items.add(new CartItemResponse(
                    p.getId(), p.getName(), p.getCode(),
                    unitPrice, qty, subtotal, p.getStock(), stockOk));
        }

        return new CartResponse(items, total, totalQuantity, valid);
    }
}
