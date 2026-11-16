package com.crsms.repository;

import com.crsms.model.resource.ResourceUnit;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceUnitRepository extends JpaRepository<ResourceUnit, Long> {
    List<ResourceUnit> findByListingId(Long listingId);

    boolean existsByAssetCodeIgnoreCase(String assetCode);

    long count();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM ResourceUnit u WHERE u.id = :id")
    Optional<ResourceUnit> findByIdForUpdate(@Param("id") Long id);
}
