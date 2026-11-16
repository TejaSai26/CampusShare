package com.crsms.service;

import com.crsms.dto.ApiDtos.BorrowingResponse;
import com.crsms.dto.ApiDtos.ReturnRequest;
import com.crsms.exception.ApiException;
import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.maintenance.MaintenanceRequest;
import com.crsms.model.payment.Payment;
import com.crsms.model.reservation.Reservation;
import com.crsms.model.user.User;
import com.crsms.pricing.PricingStrategyFactory;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.MaintenanceRequestRepository;
import com.crsms.repository.PaymentRepository;
import com.crsms.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowingService {
    private final BorrowingRecordRepository borrowings;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final MaintenanceRequestRepository maintenance;
    private final PricingStrategyFactory pricing;

    public BorrowingService(BorrowingRecordRepository borrowings, ReservationRepository reservations,
                            PaymentRepository payments, MaintenanceRequestRepository maintenance,
                            PricingStrategyFactory pricing) {
        this.borrowings = borrowings;
        this.reservations = reservations;
        this.payments = payments;
        this.maintenance = maintenance;
        this.pricing = pricing;
    }

    @Transactional
    public BorrowingResponse pickup(User actor, Long reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ApiException(404, "Reservation not found"));
        if (!reservation.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the borrower can confirm pickup");
        }
        if (reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new ApiException(400, "Pickup is allowed only after approval");
        }
        if (borrowings.findByReservationId(reservationId).isPresent()) {
            throw new ApiException(409, "This reservation was already picked up");
        }
        if (!reservation.getUnit().getStatus().isReservable()
                && reservation.getUnit().getStatus() != UnitStatus.RESERVED) {
            throw new ApiException(400, "Unit cannot be picked up in status " + reservation.getUnit().getStatus());
        }
        reservation.markPickedUp();
        reservation.getUnit().changeStatus(UnitStatus.BORROWED);
        BorrowingRecord record = borrowings.save(new BorrowingRecord(reservation));
        return toResponse(record, null);
    }

    @Transactional
    public BorrowingResponse giveBack(User actor, Long borrowingId, ReturnRequest request) {
        BorrowingRecord record = borrowings.findById(borrowingId)
                .orElseThrow(() -> new ApiException(404, "Borrowing not found"));
        if (!record.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the borrower can return this unit");
        }
        if (record.getStatus() == BorrowingStatus.RETURNED) {
            throw new ApiException(409, "This borrowing was already returned");
        }
        Instant now = Instant.now();
        boolean late = now.isAfter(record.getDueAt());
        BigDecimal charge = pricing.forType(record.getUnit().getListing().getPricingType())
                .calculate(record.getUnit().getListing().getRate(), record.getPickedUpAt(), now);
        record.completeReturn(now, charge, late);
        record.getReservation().markReturned();
        if (request != null && request.damaged()) {
            record.getUnit().changeCondition(ResourceCondition.DAMAGED);
            record.getUnit().changeStatus(UnitStatus.MAINTENANCE_REQUIRED);
            String note = request.damageNote() == null || request.damageNote().isBlank()
                    ? "Damage reported at return"
                    : request.damageNote().trim();
            maintenance.save(new MaintenanceRequest(record.getUnit(), actor, note));
        } else {
            record.getUnit().changeStatus(UnitStatus.AVAILABLE);
        }
        Payment payment = payments.save(new Payment(record, charge));
        return toResponse(record, payment.getStatus().name());
    }

    @Transactional
    public List<BorrowingResponse> mine(User user) {
        return borrowings.findByBorrowerIdOrderByPickedUpAtDesc(user.getId()).stream().map(record -> {
            record.markOverdue(Instant.now());
            return toResponse(record, null);
        }).toList();
    }

    private BorrowingResponse toResponse(BorrowingRecord record, String paymentStatus) {
        return new BorrowingResponse(
                record.getId(),
                record.getReservation().getId(),
                record.getUnit().getListing().getTitle(),
                record.getUnit().getAssetCode(),
                record.getPickedUpAt(),
                record.getDueAt(),
                record.getReturnedAt(),
                record.getChargeAmount(),
                record.isLate(),
                record.getStatus().name(),
                paymentStatus);
    }
}
