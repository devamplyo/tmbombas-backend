package com.projeto.th_piscinas_api.dto.collaborator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CollaboratorDetailResponse(
        Long id, String nome, String matricula, String perfil, boolean ativo,
        LocalDateTime criadoEm,
        long salesCount, BigDecimal totalRevenue,
        long serviceOrderCount, long completedServiceOrders,
        List<SaleHistoryItem> sales,
        List<ServiceOrderHistoryItem> serviceOrders
) {
}
