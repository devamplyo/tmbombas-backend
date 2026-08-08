package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.product.LowStockResponse;
import com.projeto.th_piscinas_api.dto.product.ProductRequest;
import com.projeto.th_piscinas_api.dto.product.ProductResponse;
import com.projeto.th_piscinas_api.dto.product.ProductSearchResponse;
import com.projeto.th_piscinas_api.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        List<ProductResponse> products = productService.findAllProducts();

        return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(product);

    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                         @Valid @RequestBody ProductRequest request) {
        ProductResponse productResponse = productService.updateProduct(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(productResponse);
    }


    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<List<LowStockResponse>> lowStock() {

        List<LowStockResponse> lowStock = productService.lowStockAlert();

        return ResponseEntity.status(HttpStatus.OK).body(lowStock);
    }

    /** Point-of-sale search: by name or internal code (typing). */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    public ResponseEntity<List<ProductSearchResponse>> searchProduct(@RequestParam("q") String q) {

        List<ProductSearchResponse> products = productService.searchProduct(q);

        return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    /** Barcode lookup: returns the exact product. */
    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
    public ResponseEntity<ProductSearchResponse> byBarcode(@PathVariable String barcode) {

        ProductSearchResponse product = productService.findByBarcode(barcode);

        return ResponseEntity.status(HttpStatus.OK).body(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADM_MASTER')")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {

        productService.deactivateProduct(id);

        return ResponseEntity.noContent().build();
    }
}
