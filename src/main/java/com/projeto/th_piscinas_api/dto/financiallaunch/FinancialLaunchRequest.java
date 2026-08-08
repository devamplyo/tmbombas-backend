package com.projeto.th_piscinas_api.dto.financiallaunch;

import com.projeto.th_piscinas_api.util.OrigemLancamento;
import com.projeto.th_piscinas_api.util.TipoLancamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialLaunchRequest(

        @NotNull TipoLancamento tipo,
        @NotNull OrigemLancamento origem,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal valor,
        @NotNull LocalDate data,
        String descricao,
        Long supplierId
) {
}
