package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.receivable.ConfirmReceivableRequest;
import com.projeto.th_piscinas_api.dto.receivable.ReceivableListResponse;
import com.projeto.th_piscinas_api.dto.receivable.ReceivableResponse;
import com.projeto.th_piscinas_api.mapper.ReceivableMapper;
import com.projeto.th_piscinas_api.model.FinancialLaunch;
import com.projeto.th_piscinas_api.model.Receivable;
import com.projeto.th_piscinas_api.repository.ReceivableRepository;
import com.projeto.th_piscinas_api.model.Sale;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.repository.FinancialLaunchRepository;
import com.projeto.th_piscinas_api.util.*;
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
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final FinancialLaunchRepository financialLaunchRepository;
    private final ReceivableMapper receivableMapper;

    @Transactional(readOnly = true)
    public ReceivableListResponse listOfReceivables(ReceivableStatus status) {
        List<Receivable> found = receivableRepository.findByStatus(
                status != null ? status : ReceivableStatus.PENDENTE);

        List<ReceivableResponse> items = found.stream().map(receivableMapper::toResponse).toList();

        BigDecimal total = items.stream()
                .map(ReceivableResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        return new ReceivableListResponse(items, total);
    }

    @Transactional
    public ReceivableResponse confirmReceivable(Long id, ConfirmReceivableRequest req) {

        Receivable r = receivableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conta a receber não encontrada: " + id));

        if (r.getStatus() == ReceivableStatus.RECEBIDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recebimento já confirmado");
        }
        if (r.getStatus() == ReceivableStatus.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conta cancelada");
        }

        LocalDate data = (req != null && req.receivedDate() != null)
                ? req.receivedDate() : LocalDate.now();
        confirm(r, req != null ? req.paymentMethod() : null, data);

        return receivableMapper.toResponse(r);
    }

    @Transactional
    public Receivable createForSale(Sale sale, PaymentType type, PaymentMethod method, LocalDate dueDate) {
        Receivable r = Receivable.builder()
                .sourceType(ReceivableSource.VENDA)
                .sourceId(sale.getId())
                .clientName(sale.getCustomerName())
                .description("Venda #" + sale.getId())
                .amount(sale.getTotal())
                .dueDate(dueDate)
                .status(ReceivableStatus.PENDENTE)
                .build();
        r = receivableRepository.save(r);

        if (type == null || type == PaymentType.A_VISTA) {

            confirm(r, method != null ? method : PaymentMethod.DINHEIRO, LocalDate.now());
        }
        return r;
    }


    /**
     * Opens the receivable for a service order that has just been completed.
     *
     * <p>Idempotent: an order whose status goes back and forth doesn't generate a
     * second entry. An order with no value has nothing to receive and is skipped —
     * {@code amount} is NOT NULL in the table.</p>
     */
    @Transactional
    public void createForServiceOrder(ServiceOrder order) {
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (receivableRepository.findBySourceTypeAndSourceId(
                ReceivableSource.ORDEM_SERVICO, order.getId()).isPresent()) {
            return;
        }

        String descricao = order.getOrderNumber();
        if (order.getTitle() != null && !order.getTitle().isBlank()) {
            descricao = descricao + " — " + order.getTitle();
        }
        if (descricao.length() > 255) {
            descricao = descricao.substring(0, 255);
        }

        receivableRepository.save(Receivable.builder()
                .sourceType(ReceivableSource.ORDEM_SERVICO)
                .sourceId(order.getId())
                .clientName(order.getClient() == null ? null : order.getClient().getName())
                .description(descricao)
                .amount(order.getPrice())
                .dueDate(LocalDate.now())
                .status(ReceivableStatus.PENDENTE)
                .build());
    }


    @Transactional
    public void removeForSale(Long saleId) {
        receivableRepository.findBySourceTypeAndSourceId(ReceivableSource.VENDA, saleId)
                .ifPresent(r -> {
                    if (r.getFinancialLaunchId() != null) {
                        financialLaunchRepository.deleteById(r.getFinancialLaunchId());
                    }
                    receivableRepository.delete(r);
                });
    }


    private void confirm(Receivable r, PaymentMethod method, LocalDate data) {

        r.setStatus(ReceivableStatus.RECEBIDO);
        r.setReceivedDate(data);
        r.setPaymentMethod(method);

        OrigemLancamento origem = (r.getSourceType() == ReceivableSource.VENDA)
                ? OrigemLancamento.VENDA : OrigemLancamento.OUTRO;

        FinancialLaunch launch = FinancialLaunch.builder()
                .tipo(TipoLancamento.ENTRADA)
                .origem(origem)
                .valor(r.getAmount())
                .data(data)
                .descricao(r.getDescription())
                .build();
        launch = financialLaunchRepository.save(launch);

        r.setFinancialLaunchId(launch.getId());
        receivableRepository.save(r);
    }


}
