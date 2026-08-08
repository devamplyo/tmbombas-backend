package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialFlowResponse;
import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialLaunchRequest;
import com.projeto.th_piscinas_api.dto.financiallaunch.FinancialLaunchResponse;
import com.projeto.th_piscinas_api.dto.financiallaunch.FlowPeriodResponse;
import com.projeto.th_piscinas_api.exception.InvalidDateException;
import com.projeto.th_piscinas_api.exception.SupplierNotFoundException;
import com.projeto.th_piscinas_api.exception.SupplierOrigenException;
import com.projeto.th_piscinas_api.mapper.FinancialLaunchMapper;
import com.projeto.th_piscinas_api.model.FinancialLaunch;
import com.projeto.th_piscinas_api.repository.FinancialLaunchRepository;
import com.projeto.th_piscinas_api.repository.SupplierRepository;
import com.projeto.th_piscinas_api.util.Granularidade;
import com.projeto.th_piscinas_api.util.OrigemLancamento;
import com.projeto.th_piscinas_api.util.TipoLancamento;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private final FinancialLaunchRepository financialLaunchRepository;
    private final SupplierRepository supplierRepository;
    private final FinancialLaunchMapper financialLaunchMapper;

    public FinancialFlowResponse fluxoPorPeriodo(LocalDate inicio,
                                                 LocalDate fim,
                                                 Granularidade granularidade) {
        if (fim == null) fim = LocalDate.now();
        if (inicio == null) inicio = fim.minusMonths(6);
        if (granularidade == null) granularidade = Granularidade.MES;

        if (inicio.isAfter(fim)) {
            throw new InvalidDateException(
                    "Data inicial não pode ser maior que a final");
        }

        List<FinancialLaunch> lancamentos =
                financialLaunchRepository.findByDataBetweenOrderByData(inicio, fim);

        // Groups by period (bucket start according to granularity), summing
        // inflows, outflows and sales. TreeMap keeps periods sorted.
        Map<LocalDate, BigDecimal[]> buckets = new TreeMap<>();
        for (FinancialLaunch l : lancamentos) {
            LocalDate periodo = truncar(l.getData(), granularidade);
            BigDecimal[] acc = buckets.computeIfAbsent(periodo,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            if (l.getTipo() == TipoLancamento.ENTRADA) acc[0] = acc[0].add(l.getValor());
            if (l.getTipo() == TipoLancamento.SAIDA)   acc[1] = acc[1].add(l.getValor());
            if (l.getOrigem() == OrigemLancamento.VENDA) acc[2] = acc[2].add(l.getValor());
        }

        List<FlowPeriodResponse> periodos = buckets.entrySet().stream()
                .map(e -> new FlowPeriodResponse(
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2],
                        e.getValue()[0].subtract(e.getValue()[1])))
                .toList();

        // totals for the entire period (for the summary cards)
        BigDecimal totEntradas = soma(periodos, FlowPeriodResponse::totalEntradas);
        BigDecimal totSaidas   = soma(periodos, FlowPeriodResponse::totalSaidas);
        BigDecimal totVendas   = soma(periodos, FlowPeriodResponse::totalVendas);

        return new FinancialFlowResponse(
                periodos, totEntradas, totSaidas, totVendas,
                totEntradas.subtract(totSaidas));
    }

    private BigDecimal soma(List<FlowPeriodResponse> lista,
                            java.util.function.Function<FlowPeriodResponse, BigDecimal> campo) {
        return lista.stream().map(campo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Start of the period the date belongs to, according to the granularity. */
    private LocalDate truncar(LocalDate data, Granularidade granularidade) {
        return switch (granularidade) {
            case DIA    -> data;
            case SEMANA -> data.with(DayOfWeek.MONDAY);
            case MES    -> data.withDayOfMonth(1);
        };
    }


    @Transactional
    public FinancialLaunchResponse createFinancialLaunch(FinancialLaunchRequest req) {

        // Rule: supplierId only makes sense for supplier-related spending.
        if (req.supplierId() != null) {
            if (req.origem() != OrigemLancamento.FORNECEDOR) {
                throw new SupplierOrigenException(
                        "supplierId só se aplica a lançamentos com origem FORNECEDOR");
            }
            if (!supplierRepository.existsById(req.supplierId())) {
                throw new SupplierNotFoundException(
                        "Fornecedor não encontrado: " + req.supplierId());
            }
        }
        // And the reverse: supplier spending without saying who it was becomes a stray data point in the chart.
        if (req.origem() == OrigemLancamento.FORNECEDOR && req.supplierId() == null) {
            throw new SupplierOrigenException(
                    "Lançamento com origem FORNECEDOR exige supplierId");
        }

        FinancialLaunch launch = financialLaunchMapper.toEntity(req);
        financialLaunchRepository.save(launch);

        return financialLaunchMapper.toResponse(launch);
    }
}
