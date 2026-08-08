package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.MaintenancePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {

    List<MaintenancePlan> findByActiveTrueAndNextMaintenanceDateLessThanEqualOrderByNextMaintenanceDateAsc(
            LocalDate limit);

    List<MaintenancePlan> findByReleasedTrueAndActiveTrueAndTechnicianIdOrderByNextMaintenanceDateAsc(
            Long technicianId);
}
