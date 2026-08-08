package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.ServiceTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceTaskRepository extends JpaRepository<ServiceTask, Long> {
    List<ServiceTask> findAllByOrderByScheduledDateDesc();
    List<ServiceTask> findByTechnicianIdOrderByScheduledDateDesc(Long technicianId);
}
