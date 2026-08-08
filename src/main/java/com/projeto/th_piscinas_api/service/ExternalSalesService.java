package com.projeto.th_piscinas_api.service;


import com.projeto.th_piscinas_api.dto.externalSales.ExternalSalesReportResponse;
import com.projeto.th_piscinas_api.dto.externalSales.ExternalSellerSummary;
import com.projeto.th_piscinas_api.exception.InvalidDateException;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ExternalSellerStatsProjection;
import com.projeto.th_piscinas_api.repository.SaleRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalSalesService {

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public ExternalSalesReportResponse externalSalesReport(LocalDate inicio, LocalDate fim) {
        if (fim == null) fim = LocalDate.now();
        if (inicio == null) inicio = fim.minusMonths(1);
        if (inicio.isAfter(fim)) {
            throw new InvalidDateException(
                    "Data inicial não pode ser maior que a final");
        }

        List<User> externos = userRepository.findByPerfil(Perfil.VENDEDOR_EXTERNO);
        if (externos.isEmpty()) {
            return new ExternalSalesReportResponse(List.of(), 0, BigDecimal.ZERO, inicio, fim);
        }

        LocalDateTime from = inicio.atStartOfDay();
        LocalDateTime to = fim.atTime(LocalTime.MAX);
        List<Long> ids = externos.stream().map(User::getId).toList();

        Map<Long, ExternalSellerStatsProjection> stats = saleRepository
                .aggregateBySellers(ids, from, to).stream()
                .collect(Collectors.toMap(ExternalSellerStatsProjection::getSellerId, Function.identity()));

        // includes ALL external salespeople, even those who sold nothing in the period
        List<ExternalSellerSummary> sellers = externos.stream()
                .map(u -> {
                    ExternalSellerStatsProjection s = stats.get(u.getId());
                    return new ExternalSellerSummary(
                            u.getId(), u.getNome(),
                            s != null ? s.getSalesCount() : 0L,
                            s != null ? s.getTotalRevenue() : BigDecimal.ZERO,
                            s != null ? s.getLastSale() : null);
                })
                .sorted(Comparator.comparing(ExternalSellerSummary::totalRevenue).reversed())
                .toList();

        long totalSales = sellers.stream().mapToLong(ExternalSellerSummary::salesCount).sum();
        BigDecimal totalRevenue = sellers.stream()
                .map(ExternalSellerSummary::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ExternalSalesReportResponse(sellers, totalSales, totalRevenue, inicio, fim);
    }
}
