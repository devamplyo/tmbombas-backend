package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.util.ServiceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    List<ServiceOrder> findByTechnicianId(Long technicianId);

    List<ServiceOrder> findByStatus(ServiceOrderStatus status);

    @Query("""
        SELECT t.id      AS technicianId,
               COUNT(so) AS orderCount,
               SUM(CASE WHEN so.completedAt IS NOT NULL THEN 1 ELSE 0 END) AS completedCount,
               MAX(so.createdAt) AS lastOrder
        FROM ServiceOrder so
        JOIN so.technician t
        GROUP BY t.id
        """)
    List<TechnicianStatsProjection> aggregateByTechnician();

    List<ServiceOrder> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    // conflict: service order of the same technician, with a window overlapping [start, end)
    @Query("""
        SELECT so FROM ServiceOrder so
        WHERE so.technician.id = :technicianId
          AND so.id <> :excludeId
          AND so.status IN :statuses
          AND so.scheduledDate IS NOT NULL
          AND so.scheduledEnd  IS NOT NULL
          AND so.scheduledDate < :end
          AND so.scheduledEnd  > :start
        """)
    List<ServiceOrder> findConflicts(@Param("technicianId") Long technicianId,
                                     @Param("excludeId") Long excludeId,
                                     @Param("statuses") List<ServiceOrderStatus> statuses,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    // schedule
    List<ServiceOrder> findByScheduledDateBetweenOrderByScheduledDateAsc(
            LocalDateTime from, LocalDateTime to);

    List<ServiceOrder> findByTechnicianIdAndScheduledDateBetweenOrderByScheduledDateAsc(
            Long technicianId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT so FROM ServiceOrder so JOIN FETCH so.client ORDER BY so.createdAt DESC")
    List<ServiceOrder> findAllWithClient();

    /** Full listing without N+1: client, technician and items come in the same query. */
    @Query("""
        SELECT DISTINCT so FROM ServiceOrder so
        LEFT JOIN FETCH so.client
        LEFT JOIN FETCH so.technician
        LEFT JOIN FETCH so.items
        ORDER BY so.createdAt DESC
        """)
    List<ServiceOrder> findAllWithClientTechnicianAndItems();
}
