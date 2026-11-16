package com.crsms.model.reservation;

import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ResourceUnit unit;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    public Reservation(User borrower, ResourceUnit unit, Instant startAt, Instant endAt) {
        if (borrower == null || unit == null) {
            throw new IllegalArgumentException("Borrower and unit are required");
        }
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.borrower = borrower;
        this.unit = unit;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = ReservationStatus.REQUESTED;
        this.createdAt = Instant.now();
    }

    public void approve() {
        transition(ReservationStatus.REQUESTED, ReservationStatus.APPROVED);
    }

    public void reject() {
        transition(ReservationStatus.REQUESTED, ReservationStatus.REJECTED);
    }

    public void cancel() {
        if (status != ReservationStatus.REQUESTED && status != ReservationStatus.APPROVED) {
            throw new IllegalStateException("Only requested or approved reservations can be cancelled");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        if (status != ReservationStatus.REQUESTED && status != ReservationStatus.APPROVED) {
            throw new IllegalStateException("Only open reservations can expire");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public void markPickedUp() {
        transition(ReservationStatus.APPROVED, ReservationStatus.ACTIVE);
    }

    public void markReturned() {
        transition(ReservationStatus.ACTIVE, ReservationStatus.RETURNED);
    }

    private void transition(ReservationStatus expected, ReservationStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Cannot move reservation from " + status + " to " + next);
        }
        this.status = next;
    }

    public Long getId() {
        return id;
    }

    public User getBorrower() {
        return borrower;
    }

    public ResourceUnit getUnit() {
        return unit;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
