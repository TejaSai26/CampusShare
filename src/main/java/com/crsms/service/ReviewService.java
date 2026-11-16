package com.crsms.service;

import com.crsms.dto.ApiDtos.ReviewRequest;
import com.crsms.dto.ApiDtos.ReviewResponse;
import com.crsms.exception.ApiException;
import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.review.Review;
import com.crsms.model.user.User;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BorrowingRecordRepository borrowings;

    public ReviewService(ReviewRepository reviews, BorrowingRecordRepository borrowings) {
        this.reviews = reviews;
        this.borrowings = borrowings;
    }

    @Transactional
    public ReviewResponse create(User reviewer, ReviewRequest request) {
        BorrowingRecord borrowing = borrowings.findById(request.borrowingId())
                .orElseThrow(() -> new ApiException(404, "Borrowing not found"));
        if (!borrowing.getBorrower().getId().equals(reviewer.getId())) {
            throw new ApiException(403, "Only the borrower can review this borrowing");
        }
        if (borrowing.getStatus() != BorrowingStatus.RETURNED) {
            throw new ApiException(400, "Reviews require a completed borrowing");
        }
        if (reviews.existsByBorrowingId(borrowing.getId())) {
            throw new ApiException(409, "This borrowing already has a review");
        }
        Review review = reviews.save(new Review(borrowing, reviewer, request.rating(), request.comment()));
        return new ReviewResponse(review.getId(), review.getRating(), review.getComment(), reviewer.getFullName());
    }
}
