package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.product.LowStockResponse;
import com.projeto.th_piscinas_api.dto.product.ProductRequest;
import com.projeto.th_piscinas_api.dto.product.ProductResponse;
import com.projeto.th_piscinas_api.dto.product.ProductSearchResponse;
import com.projeto.th_piscinas_api.exception.BarCodeNotFoundException;
import com.projeto.th_piscinas_api.exception.CodeAlreadyInUseException;
import com.projeto.th_piscinas_api.exception.ProductNotFoundException;
import com.projeto.th_piscinas_api.mapper.ProductMapper;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.util.ProductCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product buildProduct(Long id, String code, int stock, int minStock) {
        Product p = new Product();
        p.setId(id);
        p.setName("Produto Teste");
        p.setCode(code);
        p.setBarcode("7891234567890");
        p.setManufacturer("Fabricante");
        p.setPrice(new BigDecimal("99.90"));
        p.setStock(stock);
        p.setMinStock(minStock);
        p.setCategory(ProductCategory.PUMP);
        p.setActive(true);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private ProductResponse buildResponse(Long id, String code) {
        return new ProductResponse(id, "Produto Teste", null, code, "7891234567890",
                "Fabricante", new BigDecimal("99.90"), 10, 5,
                ProductCategory.PUMP, null, null, null, "un", true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void findAllProducts_returnsListMappedFromRepository() {
        Product p = buildProduct(1L, "P001", 10, 5);
        ProductResponse resp = buildResponse(1L, "P001");

        when(productRepository.findAll()).thenReturn(List.of(p));
        when(productMapper.toResponse(p)).thenReturn(resp);

        List<ProductResponse> result = productService.findAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("P001");
    }

    @Test
    void createProduct_success() {
        ProductRequest req = new ProductRequest("Nome", null, "P001", "7891234567890",
                "Fab", new BigDecimal("10.00"), 5, 2, ProductCategory.PUMP, null, null, null, "un");
        Product entity = buildProduct(1L, "P001", 5, 2);
        ProductResponse resp = buildResponse(1L, "P001");

        when(productRepository.existsByCode("P001")).thenReturn(false);
        when(productMapper.toEntity(req)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(resp);

        ProductResponse result = productService.createProduct(req);

        assertThat(result.code()).isEqualTo("P001");
        verify(productRepository).save(entity);
    }

    @Test
    void createProduct_throwsCodeAlreadyInUseException_whenCodeExists() {
        ProductRequest req = new ProductRequest("Nome", null, "P001", "7891234567890",
                "Fab", new BigDecimal("10.00"), 5, 2, ProductCategory.PUMP, null, null, null, "un");

        when(productRepository.existsByCode("P001")).thenReturn(true);

        assertThrows(CodeAlreadyInUseException.class, () -> productService.createProduct(req));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_success() {
        ProductRequest req = new ProductRequest("Nome Novo", null, "P001", "7891234567890",
                "Fab", new BigDecimal("15.00"), 8, 3, ProductCategory.FILTER, null, null, null, "un");
        Product existing = buildProduct(1L, "P001", 10, 5);
        ProductResponse resp = buildResponse(1L, "P001");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(existing)).thenReturn(resp);

        ProductResponse result = productService.updateProduct(1L, req);

        assertThat(result).isNotNull();
        verify(productMapper).updateEntity(req, existing);
        verify(productRepository).save(existing);
    }

    @Test
    void updateProduct_throwsProductNotFoundException_whenNotExists() {
        ProductRequest req = new ProductRequest("Nome", null, "P001", "7891234567890",
                "Fab", new BigDecimal("10.00"), 5, 2, ProductCategory.PUMP, null, null, null, "un");

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(99L, req));
    }

    @Test
    void lowStockAlert_returnsMappedAlerts() {
        Product p = buildProduct(1L, "P001", 2, 5);

        when(productRepository.findLowStock()).thenReturn(List.of(p));

        List<LowStockResponse> result = productService.lowStockAlert();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).shortage()).isEqualTo(3);
        assertThat(result.get(0).stock()).isEqualTo(2);
        assertThat(result.get(0).minStock()).isEqualTo(5);
    }

    @Test
    void lowStockAlert_shortageIsZero_whenStockExceedsMinStock() {
        Product p = buildProduct(1L, "P001", 10, 5);

        when(productRepository.findLowStock()).thenReturn(List.of(p));

        List<LowStockResponse> result = productService.lowStockAlert();

        assertThat(result.get(0).shortage()).isZero();
    }

    @Test
    void deleteProduct_success() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_throwsProductNotFoundException_whenNotExists() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(99L));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void searchProduct_returnsEmptyList_whenQueryIsBlank() {
        List<ProductSearchResponse> result = productService.searchProduct("  ");

        assertThat(result).isEmpty();
        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProduct_returnsEmptyList_whenQueryIsNull() {
        List<ProductSearchResponse> result = productService.searchProduct(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProduct_returnsResults_whenQueryMatches() {
        Product p = buildProduct(1L, "P001", 10, 5);
        ProductSearchResponse searchResp = new ProductSearchResponse(1L, "Produto Teste", "P001",
                "7891234567890", new BigDecimal("99.90"), 10);

        when(productRepository.searchActive("bomba")).thenReturn(List.of(p));
        when(productMapper.toSearchResponse(p)).thenReturn(searchResp);

        List<ProductSearchResponse> result = productService.searchProduct("bomba");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Produto Teste");
    }

    @Test
    void findByBarcode_success() {
        Product p = buildProduct(1L, "P001", 10, 5);
        ProductSearchResponse searchResp = new ProductSearchResponse(1L, "Produto Teste", "P001",
                "7891234567890", new BigDecimal("99.90"), 10);

        when(productRepository.findByBarcodeAndActiveTrue("7891234567890")).thenReturn(Optional.of(p));
        when(productMapper.toSearchResponse(p)).thenReturn(searchResp);

        ProductSearchResponse result = productService.findByBarcode("7891234567890");

        assertThat(result.barcode()).isEqualTo("7891234567890");
    }

    @Test
    void findByBarcode_throwsBarCodeNotFoundException_whenNotFound() {
        when(productRepository.findByBarcodeAndActiveTrue("0000000000000")).thenReturn(Optional.empty());

        assertThrows(BarCodeNotFoundException.class,
                () -> productService.findByBarcode("0000000000000"));
    }

    @Test
    void deactivateProduct_success() {
        Product p = buildProduct(1L, "P001", 10, 5);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        productService.deactivateProduct(1L);

        assertThat(p.getActive()).isFalse();
        verify(productRepository).save(p);
    }

    @Test
    void deactivateProduct_throwsProductNotFoundException_whenNotExists() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.deactivateProduct(99L));
        verify(productRepository, never()).save(any());
    }
}
