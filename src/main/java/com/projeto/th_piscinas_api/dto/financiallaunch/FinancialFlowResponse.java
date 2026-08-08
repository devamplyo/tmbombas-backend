package com.projeto.th_piscinas_api.dto.financiallaunch;

import java.math.BigDecimal;
import java.util.List;

public record FinancialFlowResponse(
        List<FlowPeriodResponse> periodos,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal totalVendas,
        BigDecimal saldo
) {
}
