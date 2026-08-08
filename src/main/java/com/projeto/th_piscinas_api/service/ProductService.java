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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> findAllProducts() {

        // ADD Pageable
       List<Product> product =  productRepository.findAll();

        return product.stream().map(productMapper::toResponse).toList();
    }

    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByCode(request.code())) {
            throw new CodeAlreadyInUseException("Código já está em uso: " + request.code());
        }
        Product productResponse = productMapper.toEntity(request);
        Product saved = productRepository.save(productResponse);

        return productMapper.toResponse(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        productMapper.updateEntity(request, product);

        Product productSaved = productRepository.save(product);

        return productMapper.toResponse(productSaved);
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStockAlert() {
        return productRepository.findLowStock().stream()
                .map(p -> new LowStockResponse(
                        p.getId(), p.getName(), p.getCode(),
                        p.getStock(), p.getMinStock(),
                        Math.max(0, p.getMinStock() - p.getStock())))
                .toList();
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductSearchResponse> searchProduct(String q) {

        if (q == null || q.isBlank()) {
            return List.of();
        }

      List<Product> searchProduct = productRepository.searchActive(q.trim());

        return searchProduct.stream().map(productMapper::toSearchResponse).toList();
    }

    /** Barcode scanned: returns the single product, ready for the cart. */
    @Transactional(readOnly = true)
    public ProductSearchResponse findByBarcode(String barcode) {

        Product p = productRepository.findByBarcodeAndActiveTrue(barcode)
                .orElseThrow(() -> new BarCodeNotFoundException("Produto não encontrado para o código: "
                        + barcode));

        return productMapper.toSearchResponse(p);
    }

    @Transactional
    public void deactivateProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado: " + id));

        product.setActive(false);

        productRepository.save(product);
    }



}
