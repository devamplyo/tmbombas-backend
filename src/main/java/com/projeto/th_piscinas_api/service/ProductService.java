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

    /** Internal/tests: fiscal fields are kept (caller already trusted). The controller uses the overload with the flag. */
    public ProductResponse createProduct(ProductRequest request) {
        return createProduct(request, true);
    }

    /**
     * @param canEditFiscal true only for the ADM Master: the fiscal fields (NCM, CFOP, origin,
     *                      CSOSN) of anyone else are dropped, so a salesperson can't change the
     *                      tax treatment of the invoices.
     */
    public ProductResponse createProduct(ProductRequest request, boolean canEditFiscal) {
        if (productRepository.existsByCode(request.code())) {
            throw new CodeAlreadyInUseException("Código já está em uso: " + request.code());
        }
        Product productResponse = productMapper.toEntity(request);
        if (!canEditFiscal) {
            productResponse.setNcm(null);
            productResponse.setCfop(null);
            productResponse.setOrigin(null);
            productResponse.setCsosn(null);
        }
        Product saved = productRepository.save(productResponse);

        return productMapper.toResponse(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        return updateProduct(id, request, true);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request, boolean canEditFiscal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        String ncm = product.getNcm();
        String cfop = product.getCfop();
        Integer origin = product.getOrigin();
        String csosn = product.getCsosn();

        productMapper.updateEntity(request, product);

        if (!canEditFiscal) {
            product.setNcm(ncm);
            product.setCfop(cfop);
            product.setOrigin(origin);
            product.setCsosn(csosn);
        }

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

    // achado F5: só existia o caminho de desativar — sem como reverter pela API.
    @Transactional
    public void activateProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado: " + id));

        product.setActive(true);

        productRepository.save(product);
    }



}
