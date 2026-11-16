package com.crsms.model.borrowing;

import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.reservation.Reservation;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "borrowing_records")
public class BorrowingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ResourceUnit unit;

    @Column(name = "picked_up_at", nullable = false)
    private Instant pickedUpAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "charge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargeAmount;

    @Column(nullable = false)
    private boolean late;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowingStatus status;

    protected BorrowingRecord() {
    }

    public BorrowingRecord(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required");
        }
        this.reservation = reservation;
        this.borrower = reservation.getBorrower();
        this.unit = reservation.getUnit();
        this.pickedUpAt = Instant.now();
        this.dueAt = reservation.getEndAt();
        this.chargeAmount = BigDecimal.ZERO;
        this.late = false;
        this.status = BorrowingStatus.ACTIVE;
    }

    public void markOverdue(Instant now) {
        if (status == BorrowingStatus.ACTIVE && now.isAfter(dueAt)) {
            this.status = BorrowingStatus.OVERDUE;
            this.late = true;
        }
    }

    public void completeReturn(Instant returnedAt, BigDecimal charge, boolean late) {
        if (status != BorrowingStatus.ACTIVE && status != BorrowingStatus.OVERDUE) {
            throw new IllegalStateException("Borrowing is already closed");
        }
        if (charge == null || charge.signum() < 0) {
            throw new IllegalArgumentException("Charge must be nonnegative");
        }
        this.returnedAt = returnedAt;
        this.chargeAmount = charge;
        this.late = late;
        this.status = BorrowingStatus.RETURNED;
    }

    public Long getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public User getBorrower() {
        return borrower;
    }

    public ResourceUnit getUnit() {
        return unit;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public BigDecimal getChargeAmount() {
        return chargeAmount;
    }

    public boolean isLate() {
        return late;
    }

    public BorrowingStatus getStatus() {
        return status;
    }
}
