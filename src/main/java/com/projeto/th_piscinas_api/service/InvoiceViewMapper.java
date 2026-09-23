package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.invoice.InvoiceItemResponse;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceResponse;
import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.model.Sale;
import com.projeto.th_piscinas_api.model.SaleItem;
import com.projeto.th_piscinas_api.repository.SaleRepository;
import com.projeto.th_piscinas_api.util.InvoiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Builds the {@link InvoiceResponse} the "Notas Fiscais" screen shows. For an NF-e the lines come
 * from the sale; for an NFS-e there is a single line (the service description).
 *
 * <p>For NF-e, {@code urlPdf}/{@code urlXml} are our own {@code /documento} endpoint (the file is
 * fetched from Focus on the server, so the token never reaches the browser). NFS-e keeps the link
 * it already had.</p>
 */
@Component
@RequiredArgsConstructor
public class InvoiceViewMapper {

    private final SaleRepository saleRepository;

    /** For the list: no line items (avoids one query per row). */
    public InvoiceResponse toSummary(Invoice inv) {
        return build(inv, List.of());
    }

    public InvoiceResponse toResponse(Invoice inv) {
        return build(inv, items(inv));
    }

    private InvoiceResponse build(Invoice inv, List<InvoiceItemResponse> items) {
        boolean nfe = inv.getDocumentType() == InvoiceType.NFE;
        String pdf = inv.getUrlPdf();
        String xml = inv.getUrlXml();
        if (nfe) {
            pdf = inv.getUrlPdf() == null ? null : "/api/invoices/" + inv.getId() + "/documento?fmt=pdf";
            xml = inv.getUrlXml() == null ? null : "/api/invoices/" + inv.getId() + "/documento?fmt=xml";
        }
        return new InvoiceResponse(
                inv.getId(), inv.getDocumentType(), inv.getReference(), inv.getSaleId(), inv.getServiceOrderId(),
                inv.getClientName(), inv.getClientDocument(), inv.getDescription(), inv.getValue(),
                inv.getStatus(), inv.getNumeroNfse(), inv.getChaveAcesso(), inv.getProtocolo(),
                pdf, xml, inv.getErrorMessage(), inv.getCreatedAt(), inv.getCancelledAt(), items);
    }

    private List<InvoiceItemResponse> items(Invoice inv) {
        if (inv.getDocumentType() == InvoiceType.NFE && inv.getSaleId() != null) {
            return saleRepository.findById(inv.getSaleId())
                    .map(sale -> itemsOf(sale))
                    .orElse(List.of());
        }
        String nome = inv.getDescription() == null || inv.getDescription().isBlank()
                ? "Prestação de serviço" : inv.getDescription();
        return List.of(new InvoiceItemResponse(nome, 1, inv.getValue(), inv.getValue()));
    }

    List<InvoiceItemResponse> itemsOf(Sale sale) {
        return sale.getItems().stream().map(this::item).toList();
    }

    private InvoiceItemResponse item(SaleItem i) {
        String nome = i.getProductName() == null ? "Produto" : i.getProductName();
        BigDecimal sub = i.getSubtotal() != null ? i.getSubtotal()
                : i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
        return new InvoiceItemResponse(nome, i.getQuantity(), i.getUnitPrice(), sub);
    }
}
