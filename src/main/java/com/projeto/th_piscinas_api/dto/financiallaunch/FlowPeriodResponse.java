package com.projeto.th_piscinas_api.dto.financiallaunch;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FlowPeriodResponse(
        LocalDate periodo,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal totalVendas,
        BigDecimal saldo
) {

}
