package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.sale.*;
import com.projeto.th_piscinas_api.exception.AuthorizationPermissionException;
import com.projeto.th_piscinas_api.exception.SaleCancelException;
import com.projeto.th_piscinas_api.exception.SaleNotFoundException;
import com.projeto.th_piscinas_api.mapper.SaleMapper;
import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.Sale;
import com.projeto.th_piscinas_api.model.SaleItem;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.repository.SaleRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ReceivableService receivableService;
    private final SaleMapper saleMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<SaleResponse> listOfSales() {

        List<Sale> sales = saleRepository.findAll();

        return sales.stream().map(saleMapper::toSaleResponse).toList();
    }


    @Transactional(readOnly = true)
    public SaleResponse findSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venda não encontrada: " + id));

        return saleMapper.toSaleResponse(sale);
    }

    @Transactional
    public SaleResponse createSale(SaleRequest req, User seller) {
        Sale sale = Sale.builder()
                .customerName(req.customerName())
                .sellerId(seller != null ? seller.getId() : null)
                .sellerName(seller != null ? seller.getNome() : null)
                .paymentMethod(req.paymentMethod())
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (SaleItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto não encontrado: " + itemReq.productId()));

            if (product.getStock() < itemReq.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Estoque insuficiente para '" + product.getName()
                                + "'. Disponível: " + product.getStock());
            }
            product.setStock(product.getStock() - itemReq.quantity());

            // achado F19: honra o preço já cotado (ex.: pedido do Vendedor
            // Externo, aprovado dias depois) em vez de buscar o preço atual
            // do produto de novo — o total que o ADM aprovou não pode
            // divergir do que efetivamente vira venda.
            BigDecimal unitPrice = itemReq.unitPrice() != null ? itemReq.unitPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(subtotal);

            sale.addItem(SaleItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(unitPrice)   // snapshot at sale time
                    .subtotal(subtotal)
                    .build());
        }

        sale.setTotal(total);
        Sale saved = saleRepository.save(sale);   // saves BEFORE: guarantees the id for the receivable

        // creates the receivable; if paid upfront (default), already confirms it and generates the entry
        receivableService.createForSale(saved, req.paymentType(), req.paymentMethod(), req.dueDate());

        return saleMapper.toSaleResponse(saved);
    }

    @Transactional
    public void deleteById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venda não encontrada: " + id));

        // returns the stock that was deducted
        for (SaleItem item : sale.getItems()) {
            Product p = item.getProduct();
            p.setStock(p.getStock() + item.getQuantity());
        }

        // Removal of the sale by id
        receivableService.removeForSale(sale.getId());

        saleRepository.delete(sale);
    }

    @Transactional
    public CancelSaleResponse cancelSale(Long saleId, CancelSaleRequest req) {
        // 1. validates the authorizing ADM's password
        User admin = autorizarComAdmin(req.adminMatricula(), req.adminPassword());

        // 2. finds the sale and prevents cancelling it twice
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException(
                        "Venda não encontrada: " + saleId));
        if (sale.getStatus() == SaleStatus.CANCELADA) {
            throw new SaleCancelException("Venda já cancelada");
        }

        // 3. returns the stock
        for (SaleItem item : sale.getItems()) {
            Product p = item.getProduct();
            p.setStock(p.getStock() + item.getQuantity());
        }

        // 4. reverses the financials (removes receivable + entry)
        receivableService.removeForSale(sale.getId());

        // 5. marks it cancelled with the audit trail
        sale.setStatus(SaleStatus.CANCELADA);
        sale.setCancelledAt(LocalDateTime.now());
        sale.setCancellationReason(req.reason());
        sale.setAuthorizedByAdminId(admin.getId());

        return saleMapper.toCancelSaleResponse(saleRepository.save(sale));
    }

    /**
     * Verifies the registration number+password of an active ADM. Generic message on purpose.
     */
    private User autorizarComAdmin(String matricula, String senha) {
        User admin = userRepository.findByMatricula(matricula)
                .orElseThrow(() -> new AuthorizationPermissionException(
                        "Autorização de administrador inválida"));
        boolean ok = admin.getPerfil() == Perfil.ADM_MASTER
                && admin.isAtivo()
                && passwordEncoder.matches(senha, admin.getSenha());
        if (!ok) {
            throw new AuthorizationPermissionException(
                    "Autorização de administrador inválida");
        }
        return admin;
    }

}
