package com.crsms.service;

import com.crsms.dto.ApiDtos.ListingRequest;
import com.crsms.dto.ApiDtos.ListingResponse;
import com.crsms.dto.ApiDtos.PageResponse;
import com.crsms.dto.ApiDtos.UnitRequest;
import com.crsms.dto.ApiDtos.UnitResponse;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.PricingType;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.resource.ResourceCategory;
import com.crsms.model.resource.ResourceListing;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.ResourceCategoryRepository;
import com.crsms.repository.ResourceListingRepository;
import com.crsms.repository.ResourceUnitRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {
    private final ResourceListingRepository listings;
    private final ResourceUnitRepository units;
    private final ResourceCategoryRepository categories;

    public ListingService(ResourceListingRepository listings, ResourceUnitRepository units,
                          ResourceCategoryRepository categories) {
        this.listings = listings;
        this.units = units;
        this.categories = categories;
    }

    @Transactional
    public ListingResponse create(User owner, ListingRequest request) {
        ResourceCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ApiException(404, "Category not found"));
        PricingType pricing = parsePricing(request.pricingType());
        ResourceListing listing = new ResourceListing(owner, category, request.title().trim(),
                request.description().trim(), pricing, request.rate());
        return toResponse(listings.save(listing), List.of());
    }

    @Transactional
    public ListingResponse update(User owner, Long id, ListingRequest request) {
        ResourceListing listing = requireOwned(owner, id);
        ResourceCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ApiException(404, "Category not found"));
        listing.edit(request.title().trim(), request.description().trim(), category,
                parsePricing(request.pricingType()), request.rate());
        return toResponse(listing, units.findByListingId(listing.getId()));
    }

    @Transactional
    public void deactivate(User owner, Long id) {
        requireOwned(owner, id).deactivate();
    }

    @Transactional
    public UnitResponse addUnit(User owner, Long listingId, UnitRequest request) {
        ResourceListing listing = requireOwned(owner, listingId);
        String code = request.assetCode() == null || request.assetCode().isBlank()
                ? "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
                : request.assetCode().trim();
        if (units.existsByAssetCodeIgnoreCase(code)) {
            throw new ApiException(409, "Asset code already exists");
        }
        ResourceCondition condition = ResourceCondition.GOOD;
        if (request.condition() != null && !request.condition().isBlank()) {
            condition = ResourceCondition.valueOf(request.condition().trim().toUpperCase(Locale.ROOT));
        }
        ResourceUnit unit = units.save(new ResourceUnit(listing, code, condition, request.locationNote()));
        return toUnit(unit);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> browse(String q, Long categoryId, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<ResourceListing> result = listings.search(query, categoryId, PageRequest.of(safePage, safeSize));
        List<ListingResponse> items = result.getContent().stream()
                .map(listing -> toResponse(listing, units.findByListingId(listing.getId())))
                .toList();
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> mine(User owner, int page, int size) {
        Page<ResourceListing> result = listings.findByOwnerId(owner.getId(), PageRequest.of(Math.max(page, 0), Math.min(size, 50)));
        List<ListingResponse> items = result.getContent().stream()
                .map(listing -> toResponse(listing, units.findByListingId(listing.getId())))
                .toList();
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ListingResponse get(Long id) {
        ResourceListing listing = listings.findById(id).orElseThrow(() -> new ApiException(404, "Listing not found"));
        return toResponse(listing, units.findByListingId(id));
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnit(Long id) {
        return toUnit(units.findById(id).orElseThrow(() -> new ApiException(404, "Unit not found")));
    }

    private ResourceListing requireOwned(User owner, Long id) {
        ResourceListing listing = listings.findById(id).orElseThrow(() -> new ApiException(404, "Listing not found"));
        if (!listing.getOwner().getId().equals(owner.getId())) {
            throw new ApiException(403, "You can only change your own listings");
        }
        return listing;
    }

    private PricingType parsePricing(String raw) {
        try {
            return PricingType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApiException(400, "Pricing must be FREE, HOURLY, or DAILY");
        }
    }

    private ListingResponse toResponse(ResourceListing listing, List<ResourceUnit> unitList) {
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory().getName(),
                listing.getCategory().getId(),
                listing.getPricingType().name(),
                listing.getRate(),
                listing.getListingStatus().name(),
                listing.getOwner().getId(),
                listing.getOwner().getFullName(),
                unitList.stream().map(this::toUnit).toList());
    }

    private UnitResponse toUnit(ResourceUnit unit) {
        return new UnitResponse(unit.getId(), unit.getAssetCode(), unit.getCondition().name(),
                unit.getStatus().name(), unit.getLocationNote());
    }

    public ListingStatus unused() {
        return ListingStatus.ACTIVE;
    }
}
