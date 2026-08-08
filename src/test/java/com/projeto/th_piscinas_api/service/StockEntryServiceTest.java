package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.stockentry.StockEntryItemRequest;
import com.projeto.th_piscinas_api.dto.stockentry.StockEntryRequest;
import com.projeto.th_piscinas_api.dto.stockentry.StockEntryResponse;
import com.projeto.th_piscinas_api.exception.SupplierNotFoundException;
import com.projeto.th_piscinas_api.mapper.StockEntryMapper;
import com.projeto.th_piscinas_api.model.FinancialLaunch;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.StockEntry;
import com.projeto.th_piscinas_api.model.Supplier;
import com.projeto.th_piscinas_api.repository.FinancialLaunchRepository;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.repository.StockEntryRepository;
import com.projeto.th_piscinas_api.repository.SupplierRepository;
import com.projeto.th_piscinas_api.util.ProductCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEntryServiceTest {

    @Mock
    private StockEntryRepository stockEntryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FinancialLaunchRepository financialLaunchRepository;

    @Mock
    private StockEntryMapper stockEntryMapper;

    @InjectMocks
    private StockEntryService stockEntryService;

    private Supplier buildSupplier(Long id) {
        return Supplier.builder()
                .id(id)
                .name("Fornecedor ABC")
                .cnpj("12345678000100")
                .active(true)
                .build();
    }

    private Product buildProduct(Long id, int currentStock) {
        Product p = new Product();
        p.setId(id);
        p.setName("Bomba d'agua");
        p.setCode("P00" + id);
        p.setPrice(new BigDecimal("100.00"));
        p.setStock(currentStock);
        p.setMinStock(2);
        p.setCategory(ProductCategory.PUMP);
        p.setActive(true);
        return p;
    }

    private StockEntry buildStockEntry(Long id) {
        return StockEntry.builder()
                .id(id)
                .supplier(buildSupplier(1L))
                .total(new BigDecimal("500.00"))
                .entryDate(LocalDate.now())
                .build();
    }

    @Test
    void listStockEntry_returnsMappedList() {
        StockEntry entry = buildStockEntry(1L);
        StockEntryResponse resp = new StockEntryResponse(1L, 1L, "Fornecedor ABC",
                "NF001", LocalDate.now(), new BigDecimal("500.00"), List.of());

        when(stockEntryRepository.findAll()).thenReturn(List.of(entry));
        when(stockEntryMapper.toResponse(entry)).thenReturn(resp);

        List<StockEntryResponse> result = stockEntryService.listStockEntry();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).supplierName()).isEqualTo("Fornecedor ABC");
    }

    @Test
    void registerStockEntry_success_increaseStockAndCreatesFinancialLaunch() {
        StockEntryItemRequest itemReq = new StockEntryItemRequest(10L, 5, new BigDecimal("100.00"));
        StockEntryRequest req = new StockEntryRequest(1L, "NF001", LocalDate.now(), List.of(itemReq));

        Supplier supplier = buildSupplier(1L);
        Product product = buildProduct(10L, 3);

        StockEntry savedEntry = buildStockEntry(1L);
        savedEntry.setId(1L);

        FinancialLaunch savedLaunch = new FinancialLaunch();
        savedLaunch.setId(100L);

        StockEntryResponse resp = new StockEntryResponse(1L, 1L, "Fornecedor ABC",
                "NF001", LocalDate.now(), new BigDecimal("500.00"), List.of());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.save(any(StockEntry.class))).thenReturn(savedEntry);
        when(financialLaunchRepository.save(any(FinancialLaunch.class))).thenReturn(savedLaunch);
        when(stockEntryMapper.toResponse(savedEntry)).thenReturn(resp);

        StockEntryResponse result = stockEntryService.registerStockEntry(req);

        assertThat(result).isNotNull();
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(product.getCostPrice()).isEqualTo(new BigDecimal("100.00"));
        verify(financialLaunchRepository).save(any(FinancialLaunch.class));
        verify(stockEntryRepository, times(2)).save(any(StockEntry.class));
    }

    @Test
    void registerStockEntry_usesTodayDate_whenEntryDateIsNull() {
        StockEntryItemRequest itemReq = new StockEntryItemRequest(10L, 2, new BigDecimal("50.00"));
        StockEntryRequest req = new StockEntryRequest(1L, null, null, List.of(itemReq));

        Supplier supplier = buildSupplier(1L);
        Product product = buildProduct(10L, 5);
        StockEntry savedEntry = buildStockEntry(1L);
        FinancialLaunch savedLaunch = new FinancialLaunch();
        savedLaunch.setId(100L);
        StockEntryResponse resp = new StockEntryResponse(1L, 1L, "Fornecedor ABC",
                null, LocalDate.now(), new BigDecimal("100.00"), List.of());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.save(any(StockEntry.class))).thenReturn(savedEntry);
        when(financialLaunchRepository.save(any(FinancialLaunch.class))).thenReturn(savedLaunch);
        when(stockEntryMapper.toResponse(savedEntry)).thenReturn(resp);

        ArgumentCaptor<StockEntry> entryCaptor = ArgumentCaptor.forClass(StockEntry.class);
        stockEntryService.registerStockEntry(req);

        verify(stockEntryRepository, atLeastOnce()).save(entryCaptor.capture());
        StockEntry captured = entryCaptor.getAllValues().get(0);
        assertThat(captured.getEntryDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void registerStockEntry_calculatesCorrectTotal_withMultipleItems() {
        StockEntryItemRequest item1 = new StockEntryItemRequest(10L, 3, new BigDecimal("100.00"));
        StockEntryItemRequest item2 = new StockEntryItemRequest(11L, 2, new BigDecimal("50.00"));
        StockEntryRequest req = new StockEntryRequest(1L, "NF002", LocalDate.now(), List.of(item1, item2));

        Supplier supplier = buildSupplier(1L);
        Product product1 = buildProduct(10L, 0);
        Product product2 = buildProduct(11L, 5);
        StockEntry savedEntry = buildStockEntry(1L);
        FinancialLaunch savedLaunch = new FinancialLaunch();
        savedLaunch.setId(200L);
        StockEntryResponse resp = new StockEntryResponse(1L, 1L, "Fornecedor ABC",
                "NF002", LocalDate.now(), new BigDecimal("400.00"), List.of());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(11L)).thenReturn(Optional.of(product2));
        when(stockEntryRepository.save(any(StockEntry.class))).thenReturn(savedEntry);
        when(financialLaunchRepository.save(any(FinancialLaunch.class))).thenReturn(savedLaunch);
        when(stockEntryMapper.toResponse(savedEntry)).thenReturn(resp);

        stockEntryService.registerStockEntry(req);

        // total = (3 * 100) + (2 * 50) = 400
        ArgumentCaptor<StockEntry> entryCaptor = ArgumentCaptor.forClass(StockEntry.class);
        verify(stockEntryRepository, atLeastOnce()).save(entryCaptor.capture());
        BigDecimal capturedTotal = entryCaptor.getAllValues().get(0).getTotal();
        assertThat(capturedTotal).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    void registerStockEntry_throwsSupplierNotFoundException_whenSupplierNotExists() {
        StockEntryRequest req = new StockEntryRequest(99L, null, null,
                List.of(new StockEntryItemRequest(1L, 1, BigDecimal.ONE)));

        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class,
                () -> stockEntryService.registerStockEntry(req));
        verify(stockEntryRepository, never()).save(any());
    }

    @Test
    void registerStockEntry_throwsSupplierNotFoundException_whenProductNotExists() {
        StockEntryItemRequest itemReq = new StockEntryItemRequest(99L, 1, new BigDecimal("10.00"));
        StockEntryRequest req = new StockEntryRequest(1L, null, null, List.of(itemReq));
        Supplier supplier = buildSupplier(1L);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // The service throws SupplierNotFoundException (reused by mistake — current behavior)
        assertThrows(SupplierNotFoundException.class,
                () -> stockEntryService.registerStockEntry(req));
    }
}
