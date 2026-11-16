package com.crsms.service;

import com.crsms.dto.ApiDtos.AnalyticsResponse;
import com.crsms.dto.ApiDtos.CategoryCount;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.MaintenanceRequestRepository;
import com.crsms.repository.ReservationRepository;
import com.crsms.repository.ResourceListingRepository;
import com.crsms.repository.ResourceUnitRepository;
import com.crsms.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final UserRepository users;
    private final ResourceListingRepository listings;
    private final ResourceUnitRepository units;
    private final ReservationRepository reservations;
    private final BorrowingRecordRepository borrowings;
    private final MaintenanceRequestRepository maintenance;

    public AnalyticsService(UserRepository users, ResourceListingRepository listings, ResourceUnitRepository units,
                            ReservationRepository reservations, BorrowingRecordRepository borrowings,
                            MaintenanceRequestRepository maintenance) {
        this.users = users;
        this.listings = listings;
        this.units = units;
        this.reservations = reservations;
        this.borrowings = borrowings;
        this.maintenance = maintenance;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse snapshot() {
        List<CategoryCount> top = borrowings.categoryCounts().stream()
                .map(row -> new CategoryCount((String) row[0], (Long) row[1]))
                .toList();
        return new AnalyticsResponse(
                users.count(),
                listings.countByListingStatus(ListingStatus.ACTIVE),
                units.count(),
                reservations.countByStatusIn(EnumSet.of(ReservationStatus.REQUESTED, ReservationStatus.APPROVED, ReservationStatus.ACTIVE)),
                borrowings.countByStatusIn(EnumSet.of(BorrowingStatus.ACTIVE, BorrowingStatus.OVERDUE)),
                borrowings.countByStatusIn(EnumSet.of(BorrowingStatus.OVERDUE)),
                maintenance.countByStatusIn(EnumSet.of(MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS)),
                top);
    }
}
