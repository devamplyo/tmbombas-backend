package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.collaborator.CollaboratorDetailResponse;
import com.projeto.th_piscinas_api.dto.collaborator.CollaboratorSummaryResponse;
import com.projeto.th_piscinas_api.dto.collaborator.SaleHistoryItem;
import com.projeto.th_piscinas_api.dto.collaborator.ServiceOrderHistoryItem;
import com.projeto.th_piscinas_api.exception.CollaboratorNotFoundException;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.*;
import com.projeto.th_piscinas_api.util.SaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    @Transactional(readOnly = true)
    public List<CollaboratorSummaryResponse> listOfSummary() {
        Map<Long, SellerStatsProjection> sellerStats = saleRepository.aggregateBySeller()
                .stream().collect(Collectors.toMap(SellerStatsProjection::getSellerId, Function.identity()));

        Map<Long, TechnicianStatsProjection> techStats = serviceOrderRepository.aggregateByTechnician()
                .stream().collect(Collectors.toMap(TechnicianStatsProjection::getTechnicianId,
                        Function.identity()));

        return userRepository.findAll().stream().map(u -> {
            SellerStatsProjection s = sellerStats.get(u.getId());
            TechnicianStatsProjection t = techStats.get(u.getId());

            long salesCount = s != null ? s.getSalesCount() : 0L;
            BigDecimal revenue = s != null ? s.getTotalRevenue() : BigDecimal.ZERO;
            long osCount = t != null ? t.getOrderCount() : 0L;
            long osCompleted = t != null ? t.getCompletedCount() : 0L;
            LocalDateTime lastActivity = latest(
                    s != null ? s.getLastSale() : null,
                    t != null ? t.getLastOrder() : null);

            return new CollaboratorSummaryResponse(
                    u.getId(), u.getNome(), u.getMatricula(), u.getPerfil().name(),
                    u.isAtivo(), u.getCriadoEm(),
                    salesCount, revenue, osCount, osCompleted, lastActivity);
        }).toList();
    }

    @Transactional(readOnly = true)
    public CollaboratorDetailResponse detail(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new CollaboratorNotFoundException(
                        "Colaborador não encontrado: " + id));

        // achado F20: excluído venda cancelada — o histórico não pode
        // aparecer como se fosse receita gerada de verdade.
        List<SaleHistoryItem> sales = saleRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(id, SaleStatus.ATIVA)
                .stream()
                .map(sale -> new SaleHistoryItem(sale.getId(), sale.getCustomerName(),
                        sale.getTotal(), sale.getCreatedAt()))
                .toList();

        List<ServiceOrderHistoryItem> orders = serviceOrderRepository
                .findByTechnicianIdOrderByCreatedAtDesc(id).stream()
                .map(so -> new ServiceOrderHistoryItem(
                        so.getId(), so.getTitle(),
                        so.getClient() != null ? so.getClient().getName() : null,
                        so.getStatus().name(), so.getCreatedAt(), so.getCompletedAt()))
                .toList();

        BigDecimal revenue = sales.stream()
                .map(SaleHistoryItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long completed = orders.stream().filter(o -> o.completedAt() != null).count();

        return new CollaboratorDetailResponse(
                u.getId(), u.getNome(), u.getMatricula(), u.getPerfil().name(),
                u.isAtivo(), u.getCriadoEm(),
                sales.size(), revenue, orders.size(), completed,
                sales, orders);
    }

    private LocalDateTime latest(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
