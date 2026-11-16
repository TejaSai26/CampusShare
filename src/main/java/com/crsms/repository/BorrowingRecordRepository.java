package com.crsms.repository;

import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.BorrowingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    Optional<BorrowingRecord> findByReservationId(Long reservationId);

    List<BorrowingRecord> findByBorrowerIdOrderByPickedUpAtDesc(Long borrowerId);

    long countByStatusIn(Collection<BorrowingStatus> statuses);

    @Query("""
            SELECT c.name, COUNT(b.id)
            FROM BorrowingRecord b
            JOIN b.unit u
            JOIN u.listing l
            JOIN l.category c
            GROUP BY c.name
            ORDER BY COUNT(b.id) DESC
            """)
    List<Object[]> categoryCounts();
}
