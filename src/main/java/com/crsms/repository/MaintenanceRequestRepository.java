package com.crsms.repository;

import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.maintenance.MaintenanceRequest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findAllByOrderByCreatedAtDesc();

    long countByStatusIn(Collection<MaintenanceStatus> statuses);
}
