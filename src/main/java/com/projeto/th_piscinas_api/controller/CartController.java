package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.cart.CartRequest;
import com.projeto.th_piscinas_api.dto.cart.CartResponse;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO', 'VENDEDOR_EXTERNO')")
public class CartController {

    private final CartService cartService;

    /** Calculates the cart: subtotals, total and stock check. Does not persist anything. */
    @PostMapping("/calculate")
    public ResponseEntity<CartResponse> calculateProductToCart(@Valid @RequestBody CartRequest req) {

        CartResponse productListToCart = cartService.calculateProductToCart(req);

        return  ResponseEntity.status(HttpStatus.OK).body(productListToCart);
    }
}
