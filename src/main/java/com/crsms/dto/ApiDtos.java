package com.crsms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 80) String password,
            @NotBlank String role,
            String department,
            String memberId,
            String designation
    ) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record ProfileUpdate(@NotBlank String fullName, String department) {
    }

    public record UserResponse(Long id, String fullName, String email, String role, String accountStatus,
                                String department, String memberLabel) {
    }

    public record CategoryResponse(Long id, String name) {
    }

    public record ListingRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotNull Long categoryId,
            @NotBlank String pricingType,
            @NotNull BigDecimal rate
    ) {
    }

    public record UnitRequest(String assetCode, String condition, String locationNote) {
    }

    public record UnitResponse(Long id, String assetCode, String condition, String status, String locationNote) {
    }

    public record ListingResponse(Long id, String title, String description, String category, Long categoryId,
                                   String pricingType, BigDecimal rate, String listingStatus,
                                   Long ownerId, String ownerName, List<UnitResponse> units) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }

    public record ReservationRequest(@NotNull Long unitId, @NotNull Instant startAt, @NotNull Instant endAt) {
    }

    public record ReservationResponse(Long id, Long unitId, String assetCode, String listingTitle,
                                       Long borrowerId, String borrowerName, Long ownerId,
                                       Instant startAt, Instant endAt, String status) {
    }

    public record BorrowingResponse(Long id, Long reservationId, String listingTitle, String assetCode,
                                     Instant pickedUpAt, Instant dueAt, Instant returnedAt,
                                     BigDecimal chargeAmount, boolean late, String status, String paymentStatus) {
    }

    public record ReturnRequest(boolean damaged, String damageNote) {
    }

    public record MaintenanceCreate(@NotNull Long unitId, @NotBlank String description) {
    }

    public record MaintenanceUpdate(String status, String unitStatus, String condition, String adminNote) {
    }

    public record MaintenanceResponse(Long id, Long unitId, String assetCode, String listingTitle,
                                       String reporterName, String description, String status,
                                       String adminNote, String unitStatus, Instant createdAt) {
    }

    public record ReviewRequest(@NotNull Long borrowingId, @Min(1) @Max(5) int rating, String comment) {
    }

    public record ReviewResponse(Long id, int rating, String comment, String reviewerName) {
    }

    public record StatusUpdate(@NotBlank String accountStatus) {
    }

    public record AnalyticsResponse(long users, long activeListings, long units, long activeReservations,
                                     long currentBorrowings, long overdueBorrowings, long pendingMaintenance,
                                     List<CategoryCount> topCategories) {
    }

    public record CategoryCount(String category, long borrowings) {
    }
}
