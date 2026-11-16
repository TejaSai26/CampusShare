package com.crsms.repository;

import com.crsms.model.enums.ListingStatus;
import com.crsms.model.resource.ResourceListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceListingRepository extends JpaRepository<ResourceListing, Long> {
    Page<ResourceListing> findByOwnerId(Long ownerId, Pageable pageable);

    long countByListingStatus(ListingStatus status);

    @Query("""
            SELECT l FROM ResourceListing l
            WHERE l.listingStatus = com.crsms.model.enums.ListingStatus.ACTIVE
              AND (:categoryId IS NULL OR l.category.id = :categoryId)
              AND (:q IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<ResourceListing> search(@Param("q") String q, @Param("categoryId") Long categoryId, Pageable pageable);
}
