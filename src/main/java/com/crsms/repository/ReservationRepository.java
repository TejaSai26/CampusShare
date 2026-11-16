package com.crsms.repository;

import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.reservation.Reservation;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.unit.listing.owner.id = :ownerId
            ORDER BY r.createdAt DESC
            """)
    List<Reservation> findIncoming(@Param("ownerId") Long ownerId);

    long countByStatusIn(Collection<ReservationStatus> statuses);

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.unit.id = :unitId
              AND r.status IN :statuses
              AND r.startAt < :endAt
              AND r.endAt > :startAt
            """)
    long countOverlapping(@Param("unitId") Long unitId,
                           @Param("startAt") Instant startAt,
                           @Param("endAt") Instant endAt,
                           @Param("statuses") Collection<ReservationStatus> statuses);
}
