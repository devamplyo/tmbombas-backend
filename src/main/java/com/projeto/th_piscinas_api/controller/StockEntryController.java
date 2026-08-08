package com.projeto.th_piscinas_api.controller;

import com.projeto.th_piscinas_api.dto.stockentry.StockEntryRequest;
import com.projeto.th_piscinas_api.dto.stockentry.StockEntryResponse;
import com.projeto.th_piscinas_api.service.StockEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-entries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADM_MASTER','VENDEDOR_INTERNO')")
public class StockEntryController {

    private final StockEntryService stockEntryService;

    @GetMapping
    public ResponseEntity<List<StockEntryResponse>> listOfStockEntries() {

        List<StockEntryResponse> stockEntryResponses = stockEntryService.listStockEntry();

        return ResponseEntity.status(HttpStatus.OK).body(stockEntryResponses);
    }

    @PostMapping
    public ResponseEntity<StockEntryResponse> registerStockEntry(@Valid
                                                                     @RequestBody
                                                                     StockEntryRequest req) {
        StockEntryResponse response = stockEntryService.registerStockEntry(req);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
