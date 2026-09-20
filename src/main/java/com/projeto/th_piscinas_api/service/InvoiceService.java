package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.config.NfseProperties;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceItemResponse;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceResponse;
import com.projeto.th_piscinas_api.dto.invoice.InvoiceStatusResponse;
import com.projeto.th_piscinas_api.dto.invoice.NfeEmitRequest;
import com.projeto.th_piscinas_api.dto.invoice.PendingInvoiceResponse;
import com.projeto.th_piscinas_api.dto.nfse.NfseEmitRequest;
import com.projeto.th_piscinas_api.dto.nfse.NfseResponse;
import com.projeto.th_piscinas_api.model.Client;
import com.projeto.th_piscinas_api.model.Invoice;
import com.projeto.th_piscinas_api.model.Sale;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.repository.InvoiceRepository;
import com.projeto.th_piscinas_api.repository.SaleRepository;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.util.InvoiceType;
import com.projeto.th_piscinas_api.util.NfseStatus;
import com.projeto.th_piscinas_api.util.SaleStatus;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One entry point for the "Notas Fiscais" screen: lists NF-e and NFS-e together, finds what is
 * still waiting for a note, and routes issue/lookup/cancel to {@link NfeService} or
 * {@link NfseService} by the type of the document.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SaleRepository saleRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final NfeService nfeService;
    private final NfseService nfseService;
    private final InvoiceViewMapper view;
    private final NfseProperties nfse;
    private final FocusNfeClient focus;

    /** A downloadable document (DANFE / XML) ready to stream. */
    public record DocumentFile(byte[] body, String contentType, String filename) {
    }

    @Transactional(readOnly = true)
    public InvoiceStatusResponse status() {
        String pendencia = nfeService.pendencia();
        return new InvoiceStatusResponse(nfse.getAmbiente(), nfse.isSimulate(),
                !nfse.isSimulate() && pendencia == null, pendencia);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list() {
        return invoiceRepository.findTop500ByOrderByCreatedAtDesc().stream().map(view::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id) {
        return view.toResponse(findInvoice(id));
    }

    // ---------- waiting for a note ----------

    @Transactional(readOnly = true)
    public List<PendingInvoiceResponse> pending() {
        List<PendingInvoiceResponse> out = new ArrayList<>();

        for (Sale sale : saleRepository.findTop200ByStatusOrderByCreatedAtDesc(SaleStatus.ATIVA)) {
            if (invoiceRepository.existsBySaleIdAndDocumentTypeAndStatusIn(sale.getId(), InvoiceType.NFE, NfeService.ATIVOS)) {
                continue;
            }
            List<InvoiceItemResponse> items = view.itemsOf(sale);
            String nome = sale.getCustomerName() == null || sale.getCustomerName().isBlank()
                    ? "Consumidor (balcão)" : sale.getCustomerName();
            out.add(new PendingInvoiceResponse(InvoiceType.NFE, sale.getId(), "Venda #" + sale.getId(),
                    null, nome, null, false, sale.getTotal(), items, sale.getCreatedAt()));
        }

        for (ServiceOrder os : serviceOrderRepository.findByStatus(ServiceOrderStatus.CONCLUIDA)) {
            if (os.getPrice() == null || os.getPrice().compareTo(BigDecimal.ZERO) <= 0) continue;
            String osId = String.valueOf(os.getId());
            if (invoiceRepository.existsByServiceOrderIdAndStatusIn(osId, NfeService.ATIVOS)) continue;
            Client c = os.getClient();
            String desc = os.getDescription() == null || os.getDescription().isBlank() ? os.getTitle() : os.getDescription();
            out.add(new PendingInvoiceResponse(InvoiceType.NFSE, os.getId(), "OS #" + os.getId(),
                    c == null ? null : c.getId(), c == null ? "Sem cliente" : c.getName(),
                    c == null ? null : c.getDocument(), c != null, os.getPrice(),
                    List.of(new InvoiceItemResponse(desc, 1, os.getPrice(), os.getPrice())), os.getCompletedAt()));
        }

        out.sort(Comparator.comparing(PendingInvoiceResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    // ---------- issue / lookup / cancel, routed by type ----------

    @Transactional
    public InvoiceResponse emitNfe(NfeEmitRequest req) {
        return nfeService.emit(req);
    }

    @Transactional
    public InvoiceResponse emitNfse(NfseEmitRequest req) {
        NfseResponse r = nfseService.emit(req);
        return view.toResponse(findInvoice(r.id()));
    }

    @Transactional
    public InvoiceResponse consult(Long id) {
        Invoice inv = findInvoice(id);
        if (inv.getDocumentType() == InvoiceType.NFE) {
            return nfeService.consult(id);
        }
        nfseService.consult(id);
        return view.toResponse(findInvoice(id));
    }

    @Transactional
    public InvoiceResponse cancel(Long id, String justificativa) {
        Invoice inv = findInvoice(id);
        if (inv.getDocumentType() == InvoiceType.NFE) {
            return nfeService.cancel(id, justificativa);
        }
        nfseService.cancel(id, justificativa);
        return view.toResponse(findInvoice(id));
    }

    // ---------- documents (NF-e) ----------

    /**
     * DANFE/XML of an NF-e. In simulation mode it is generated here ("NO FISCAL VALUE"); in real
     * mode the server fetches the file from Focus with the token, so the token never reaches the browser.
     */
    @Transactional(readOnly = true)
    public DocumentFile document(Long id, String fmt) {
        Invoice inv = findInvoice(id);
        if (inv.getDocumentType() != InvoiceType.NFE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use o link da própria NFS-e.");
        }
        boolean xml = "xml".equalsIgnoreCase(fmt);
        String numero = inv.getNumeroNfse() == null ? String.valueOf(inv.getId()) : inv.getNumeroNfse();

        if (nfse.isSimulate()) {
            return xml
                    ? new DocumentFile(nfeService.simuladoXml(inv).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "application/xml", "nfe-" + numero + "-simulada.xml")
                    : new DocumentFile(nfeService.simuladoHtml(inv).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "text/html;charset=UTF-8", "nfe-" + numero + "-simulada.html");
        }

        String url = xml ? inv.getUrlXml() : inv.getUrlPdf();
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O documento ainda não está disponível. Atualize o status da nota.");
        }
        try {
            FocusNfeClient.FileResult file = focus.download(url);
            return new DocumentFile(file.body(), file.contentType(), "nfe-" + numero + (xml ? ".xml" : ".pdf"));
        } catch (IOException e) {
            log.warn("Falha ao baixar documento da NF-e {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível baixar o arquivo da Focus agora. Tente de novo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O download foi interrompido. Tente de novo.");
        }
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota fiscal não encontrada."));
    }
}
