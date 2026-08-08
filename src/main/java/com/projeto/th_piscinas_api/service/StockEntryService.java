package com.projeto.th_piscinas_api.service;


import com.projeto.th_piscinas_api.dto.stockentry.StockEntryItemRequest;
import com.projeto.th_piscinas_api.dto.stockentry.StockEntryRequest;
import com.projeto.th_piscinas_api.dto.stockentry.StockEntryResponse;
import com.projeto.th_piscinas_api.exception.ProductNotFoundException;
import com.projeto.th_piscinas_api.exception.SupplierNotFoundException;
import com.projeto.th_piscinas_api.mapper.StockEntryMapper;
import com.projeto.th_piscinas_api.model.*;
import com.projeto.th_piscinas_api.repository.FinancialLaunchRepository;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.repository.StockEntryRepository;
import com.projeto.th_piscinas_api.repository.SupplierRepository;
import com.projeto.th_piscinas_api.util.OrigemLancamento;
import com.projeto.th_piscinas_api.util.TipoLancamento;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockEntryService {


    private final StockEntryRepository stockEntryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final FinancialLaunchRepository financialLaunchRepository;
    private final StockEntryMapper stockEntryMapper;


    @Transactional(readOnly = true)
    public List<StockEntryResponse> listStockEntry() {

        List<StockEntry> stockEntryList = stockEntryRepository.findAll();

        return stockEntryList.stream().map(stockEntryMapper::toResponse).toList();
    }

    @Transactional
    public StockEntryResponse registerStockEntry(StockEntryRequest req) {
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new SupplierNotFoundException(
                        "Fornecedor não encontrado: " + req.supplierId()));

        LocalDate data = (req.entryDate() != null) ? req.entryDate() : LocalDate.now();

        StockEntry entry = StockEntry.builder()
                .supplier(supplier)
                .documentNumber(req.documentNumber())
                .entryDate(data)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (StockEntryItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new SupplierNotFoundException("Produto não encontrado: "
                            + itemReq.productId()));
            product.setStock(product.getStock() + itemReq.quantity());
            product.setCostPrice(itemReq.unitCost());

            BigDecimal subtotal = itemReq.unitCost()
                    .multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(subtotal);

            entry.addItem(StockEntryItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitCost(itemReq.unitCost())
                    .subtotal(subtotal)
                    .build());
        }

        entry.setTotal(total);
        StockEntry saved = stockEntryRepository.save(entry);

        FinancialLaunch launch = FinancialLaunch.builder()
                .tipo(TipoLancamento.SAIDA)
                .origem(OrigemLancamento.FORNECEDOR)
                .valor(total)
                .data(data)
                .descricao("Entrada de estoque #" + saved.getId() + " - " + supplier.getName())
                .supplierId(supplier.getId())
                .build();
        launch = financialLaunchRepository.save(launch);

        saved.setFinancialLaunchId(launch.getId());
        return stockEntryMapper.toResponse(stockEntryRepository.save(saved));
    }


}
