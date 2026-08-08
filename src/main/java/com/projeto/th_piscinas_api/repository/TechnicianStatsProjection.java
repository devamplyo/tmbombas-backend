package com.projeto.th_piscinas_api.repository;

import java.time.LocalDateTime;

public interface TechnicianStatsProjection {

    Long getTechnicianId();
    Long getOrderCount();
    Long getCompletedCount();
    LocalDateTime getLastOrder();
}
