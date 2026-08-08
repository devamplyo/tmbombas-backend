package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.supplier.SupplierSpendingItem;
import com.projeto.th_piscinas_api.dto.supplier.SupplierSpendingResponse;
import com.projeto.th_piscinas_api.exception.InvalidDateException;
import com.projeto.th_piscinas_api.repository.FinancialLaunchRepository;
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
public class SupplierReportService {

    private final FinancialLaunchRepository financialLaunchRepository;

    @Transactional(readOnly = true)
    public SupplierSpendingResponse spentBySupplier(LocalDate inicio, LocalDate fim) {
        if (fim == null) fim = LocalDate.now();
        if (inicio == null) inicio = fim.minusMonths(6);
        if (inicio.isAfter(fim)) {
            throw new InvalidDateException(
                    "Data inicial não pode ser maior que a final");
        }

        List<SupplierSpendingItem> items = financialLaunchRepository
                .gastoPorFornecedor(inicio, fim).stream()
                .map(p -> new SupplierSpendingItem(
                        p.getSupplierId(), p.getSupplierName(),
                        p.getTotalSpent(), p.getLaunchCount()))
                .toList();

        BigDecimal total = items.stream()
                .map(SupplierSpendingItem::totalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SupplierSpendingResponse(items, total);
    }
}
