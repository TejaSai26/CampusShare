package com.crsms.repository;

import com.crsms.model.review.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBorrowingId(Long borrowingId);

    boolean existsByBorrowingId(Long borrowingId);
}
