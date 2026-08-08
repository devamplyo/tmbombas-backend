package com.projeto.th_piscinas_api.dto.financiallaunch;

import com.projeto.th_piscinas_api.util.OrigemLancamento;
import com.projeto.th_piscinas_api.util.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialLaunchResponse(
        Long id,
        TipoLancamento tipo,
        OrigemLancamento origem,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        Long supplierId,
        LocalDateTime criadoEm
) {
}
