package com.projeto.th_piscinas_api.dto.collaborator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CollaboratorSummaryResponse(
        Long id,
        String nome,
        String matricula,
        String perfil,
        boolean ativo,            // status
        LocalDateTime criadoEm,
        long salesCount,          // sales history (salespeople)
        BigDecimal totalRevenue,
        long serviceOrderCount,   // service order history (technicians)
        long completedServiceOrders,
        LocalDateTime lastActivity
) {
}
