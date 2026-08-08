package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.cart.CartItemRequest;
import com.projeto.th_piscinas_api.dto.cart.CartRequest;
import com.projeto.th_piscinas_api.dto.cart.CartResponse;
import com.projeto.th_piscinas_api.exception.ProductDisableException;
import com.projeto.th_piscinas_api.exception.ProductNotFoundException;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.util.ProductCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Product buildProduct(Long id, BigDecimal price, int stock, boolean active) {
        Product p = new Product();
        p.setId(id);
        p.setName("Produto " + id);
        p.setCode("P00" + id);
        p.setPrice(price);
        p.setStock(stock);
        p.setMinStock(1);
        p.setCategory(ProductCategory.PUMP);
        p.setActive(active);
        return p;
    }

    @Test
    void calculateProductToCart_success_singleItem() {
        Product p = buildProduct(1L, new BigDecimal("50.00"), 10, true);
        CartRequest req = new CartRequest(List.of(new CartItemRequest(1L, 2)));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        CartResponse result = cartService.calculateProductToCart(req);

        assertThat(result.items()).hasSize(1);
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.totalQuantity()).isEqualTo(2);
        assertThat(result.valid()).isTrue();
        assertThat(result.items().get(0).stockOk()).isTrue();
    }

    @Test
    void calculateProductToCart_mergesDuplicateProducts() {
        Product p = buildProduct(1L, new BigDecimal("30.00"), 5, true);
        CartRequest req = new CartRequest(List.of(
                new CartItemRequest(1L, 2),
                new CartItemRequest(1L, 1)
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        CartResponse result = cartService.calculateProductToCart(req);

        // 2+1 = 3 units of the same product
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(3);
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    void calculateProductToCart_markStockOkFalse_whenInsufficientStock() {
        Product p = buildProduct(1L, new BigDecimal("25.00"), 2, true);
        CartRequest req = new CartRequest(List.of(new CartItemRequest(1L, 5)));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        CartResponse result = cartService.calculateProductToCart(req);

        assertThat(result.valid()).isFalse();
        assertThat(result.items().get(0).stockOk()).isFalse();
    }

    @Test
    void calculateProductToCart_multipleItems_totalIsSum() {
        Product p1 = buildProduct(1L, new BigDecimal("100.00"), 10, true);
        Product p2 = buildProduct(2L, new BigDecimal("50.00"), 10, true);
        CartRequest req = new CartRequest(List.of(
                new CartItemRequest(1L, 2),
                new CartItemRequest(2L, 3)
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));

        CartResponse result = cartService.calculateProductToCart(req);

        assertThat(result.items()).hasSize(2);
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(result.totalQuantity()).isEqualTo(5);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void calculateProductToCart_throwsProductNotFoundException_whenProductNotExists() {
        CartRequest req = new CartRequest(List.of(new CartItemRequest(99L, 1)));

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> cartService.calculateProductToCart(req));
    }

    @Test
    void calculateProductToCart_throwsProductDisableException_whenProductIsInactive() {
        Product p = buildProduct(1L, new BigDecimal("50.00"), 10, false);
        CartRequest req = new CartRequest(List.of(new CartItemRequest(1L, 1)));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(ProductDisableException.class,
                () -> cartService.calculateProductToCart(req));
    }

    @Test
    void calculateProductToCart_validIsFalse_whenAnyItemHasInsufficientStock() {
        Product p1 = buildProduct(1L, new BigDecimal("10.00"), 10, true);
        Product p2 = buildProduct(2L, new BigDecimal("20.00"), 1, true);
        CartRequest req = new CartRequest(List.of(
                new CartItemRequest(1L, 3),
                new CartItemRequest(2L, 5) // insufficient stock
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));

        CartResponse result = cartService.calculateProductToCart(req);

        assertThat(result.valid()).isFalse();
        assertThat(result.items()).anySatisfy(i -> assertThat(i.stockOk()).isFalse());
        assertThat(result.items()).anySatisfy(i -> assertThat(i.stockOk()).isTrue());
    }
}
