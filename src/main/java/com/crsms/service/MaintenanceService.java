package com.crsms.service;

import com.crsms.dto.ApiDtos.MaintenanceCreate;
import com.crsms.dto.ApiDtos.MaintenanceResponse;
import com.crsms.dto.ApiDtos.MaintenanceUpdate;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.maintenance.MaintenanceRequest;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.MaintenanceRequestRepository;
import com.crsms.repository.ResourceUnitRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {
    private final MaintenanceRequestRepository requests;
    private final ResourceUnitRepository units;

    public MaintenanceService(MaintenanceRequestRepository requests, ResourceUnitRepository units) {
        this.requests = requests;
        this.units = units;
    }

    @Transactional
    public MaintenanceResponse report(User reporter, MaintenanceCreate body) {
        ResourceUnit unit = units.findById(body.unitId())
                .orElseThrow(() -> new ApiException(404, "Resource unit not found"));
        unit.changeCondition(ResourceCondition.DAMAGED);
        if (unit.getStatus() != UnitStatus.BORROWED) {
            unit.changeStatus(UnitStatus.MAINTENANCE_REQUIRED);
        }
        MaintenanceRequest saved = requests.save(new MaintenanceRequest(unit, reporter, body.description().trim()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> list() {
        return requests.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public MaintenanceResponse update(Long id, MaintenanceUpdate body) {
        MaintenanceRequest request = requests.findById(id)
                .orElseThrow(() -> new ApiException(404, "Maintenance request not found"));
        MaintenanceStatus status = body.status() == null || body.status().isBlank()
                ? null
                : MaintenanceStatus.valueOf(body.status().trim().toUpperCase(Locale.ROOT));
        request.update(status, body.adminNote());
        ResourceUnit unit = request.getUnit();
        if (body.condition() != null && !body.condition().isBlank()) {
            unit.changeCondition(ResourceCondition.valueOf(body.condition().trim().toUpperCase(Locale.ROOT)));
        }
        if (body.unitStatus() != null && !body.unitStatus().isBlank()) {
            unit.changeStatus(UnitStatus.valueOf(body.unitStatus().trim().toUpperCase(Locale.ROOT)));
        } else if (status == MaintenanceStatus.IN_PROGRESS) {
            unit.changeStatus(UnitStatus.UNDER_REPAIR);
        } else if (status == MaintenanceStatus.RESOLVED && unit.getStatus() != UnitStatus.BORROWED) {
            unit.changeStatus(UnitStatus.AVAILABLE);
        }
        return toResponse(request);
    }

    private MaintenanceResponse toResponse(MaintenanceRequest request) {
        return new MaintenanceResponse(
                request.getId(),
                request.getUnit().getId(),
                request.getUnit().getAssetCode(),
                request.getUnit().getListing().getTitle(),
                request.getReporter().getFullName(),
                request.getDescription(),
                request.getStatus().name(),
                request.getAdminNote(),
                request.getUnit().getStatus().name(),
                request.getCreatedAt());
    }
}
