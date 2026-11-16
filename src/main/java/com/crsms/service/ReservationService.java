package com.crsms.service;

import com.crsms.dto.ApiDtos.ReservationRequest;
import com.crsms.dto.ApiDtos.ReservationResponse;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.reservation.Reservation;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.ReservationRepository;
import com.crsms.repository.ResourceUnitRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    static final EnumSet<ReservationStatus> BLOCKING = EnumSet.of(
            ReservationStatus.REQUESTED, ReservationStatus.APPROVED, ReservationStatus.ACTIVE);

    private final ReservationRepository reservations;
    private final ResourceUnitRepository units;

    public ReservationService(ReservationRepository reservations, ResourceUnitRepository units) {
        this.reservations = reservations;
        this.units = units;
    }

    @Transactional
    public ReservationResponse request(User borrower, ReservationRequest request) {
        ResourceUnit unit = units.findByIdForUpdate(request.unitId())
                .orElseThrow(() -> new ApiException(404, "Resource unit not found"));
        if (unit.getListing().getOwner().getId().equals(borrower.getId())) {
            throw new ApiException(400, "You cannot reserve a unit you own");
        }
        if (unit.getListing().getListingStatus() != ListingStatus.ACTIVE) {
            throw new ApiException(400, "This listing is not active");
        }
        if (!unit.getStatus().isReservable()) {
            throw new ApiException(400, "This unit is unavailable (" + unit.getStatus() + ")");
        }
        if (request.startAt() == null || request.endAt() == null || !request.endAt().isAfter(request.startAt())) {
            throw new ApiException(400, "End time must be after start time");
        }
        if (request.startAt().isBefore(Instant.now().minusSeconds(120))) {
            throw new ApiException(400, "Reservations cannot start in the past");
        }
        long overlaps = reservations.countOverlapping(unit.getId(), request.startAt(), request.endAt(), BLOCKING);
        if (overlaps > 0) {
            throw new ApiException(409, "That unit is already reserved for an overlapping period");
        }
        Reservation reservation = reservations.save(new Reservation(borrower, unit, request.startAt(), request.endAt()));
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse approve(User actor, Long id) {
        Reservation reservation = load(id);
        assertOwner(actor, reservation);
        reservation.approve();
        if (reservation.getUnit().getStatus() == UnitStatus.AVAILABLE) {
            reservation.getUnit().changeStatus(UnitStatus.RESERVED);
        }
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse reject(User actor, Long id) {
        Reservation reservation = load(id);
        assertOwner(actor, reservation);
        reservation.reject();
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancel(User actor, Long id) {
        Reservation reservation = load(id);
        boolean borrower = reservation.getBorrower().getId().equals(actor.getId());
        boolean owner = reservation.getUnit().getListing().getOwner().getId().equals(actor.getId());
        if (!borrower && !owner) {
            throw new ApiException(403, "You cannot cancel this reservation");
        }
        boolean wasApproved = reservation.getStatus() == ReservationStatus.APPROVED;
        reservation.cancel();
        if (wasApproved && reservation.getUnit().getStatus() == UnitStatus.RESERVED) {
            reservation.getUnit().changeStatus(UnitStatus.AVAILABLE);
        }
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(User user) {
        return reservations.findByBorrowerIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> incoming(User owner) {
        return reservations.findIncoming(owner.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(User user, Long id) {
        Reservation reservation = load(id);
        boolean borrower = reservation.getBorrower().getId().equals(user.getId());
        boolean owner = reservation.getUnit().getListing().getOwner().getId().equals(user.getId());
        if (!borrower && !owner && user.getRole() != com.crsms.model.enums.UserRole.ADMIN) {
            throw new ApiException(403, "You cannot view this reservation");
        }
        return toResponse(reservation);
    }

    Reservation load(Long id) {
        return reservations.findById(id).orElseThrow(() -> new ApiException(404, "Reservation not found"));
    }

    private void assertOwner(User actor, Reservation reservation) {
        if (reservation.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "You cannot approve or reject your own request");
        }
        if (!reservation.getUnit().getListing().getOwner().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the owner can decide this request");
        }
    }

    ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUnit().getId(),
                reservation.getUnit().getAssetCode(),
                reservation.getUnit().getListing().getTitle(),
                reservation.getBorrower().getId(),
                reservation.getBorrower().getFullName(),
                reservation.getUnit().getListing().getOwner().getId(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getStatus().name());
    }
}
