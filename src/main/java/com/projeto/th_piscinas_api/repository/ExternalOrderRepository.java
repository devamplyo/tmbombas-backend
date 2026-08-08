package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.ExternalOrder;
import com.projeto.th_piscinas_api.util.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalOrderRepository extends JpaRepository<ExternalOrder, Long> {

    List<ExternalOrder> findByStatus(OrderStatus status);
    List<ExternalOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
