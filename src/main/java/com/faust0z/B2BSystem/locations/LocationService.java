package com.faust0z.B2BSystem.locations;


import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.locations.LocationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.locations.LocationResponse;
import com.faust0z.B2BSystem.locations.LocationMapper;
import com.faust0z.B2BSystem.locations.LocationService;
import com.faust0z.B2BSystem.locations.Location;
import com.faust0z.B2BSystem.locations.LocationRepository;
import com.faust0z.B2BSystem.locations.LocationCreateRequest;
import com.faust0z.B2BSystem.locations.LocationDisabledException;
import com.faust0z.B2BSystem.locations.LocationNameExistsException;
import com.faust0z.B2BSystem.locations.LocationNotFoundException;
import com.faust0z.B2BSystem.locations.LocationUpdateRequest;
@Slf4j
@Service
public class LocationService implements LocationInternalApi {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    LocationService(LocationRepository locationRepository, LocationMapper locationMapper) {
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
    }

    @Override
    @Cacheable(value = "branches", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocations() {
        log.debug("Querying locations");
        return locationRepository.findAllBy(LocationResponse.class);
    }

    @CacheEvict(value = "branches", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional
    public LocationResponse createLocation(LocationCreateRequest request) {
        log.debug("Creating location | name={}", request.name());

        if (locationRepository.existsByNameIgnoreCase(request.name())) throw new LocationNameExistsException();

        Location savedLocation = locationRepository.saveAndFlush(locationMapper.toEntity(request));

        log.info("Location created | locationId: {} | name: {}", savedLocation.getId(), savedLocation.getName());
        return locationMapper.toResponse(savedLocation);
    }

    @Override
    @CacheEvict(value = "branches", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional
    public LocationResponse createDefaultLocation() {
        Location location = new Location();
        location.setName("Default Location");
        Location savedLocation = locationRepository.save(location);

        log.info("Default location created | locationId: {} | name: {}", savedLocation.getId(), savedLocation.getName());
        return locationMapper.toResponse(savedLocation);
    }

    @Caching(evict = {
            @CacheEvict(value = "branches", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "branch_active_validation", key = "#locationId")
    })
    @Transactional
    public LocationResponse updateLocation(UUID locationId, LocationUpdateRequest request) {
        log.debug("Updating location | locationId={}", locationId);

        Location locationEntity = locationRepository.findById(locationId).orElseThrow(LocationNotFoundException::new);

        if (request.name() != null && !locationRepository.existsByNameIgnoreCase(locationEntity.getName())) {
            throw new LocationNameExistsException();
        }

        locationMapper.updateFromRequest(request, locationEntity);
        locationRepository.save(locationEntity);

        log.debug("Location updated | locationId={}", locationId);
        return locationMapper.toResponse(locationEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "branches", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "branch_active_validation", key = "#locationId")
    })
    @Transactional
    public LocationResponse toggleLocationStatus(UUID locationId) {
        log.debug("Toggling location status | locationId={}", locationId);

        Location locationEntity = locationRepository.findById(locationId).orElseThrow(LocationNotFoundException::new);

        if (locationEntity.getDeletedAt() == null) {
            locationEntity.softDelete();
        } else {
            locationEntity.setDeletedAt(null);
        }

        log.info("Location status toggled | locationId={} | deletedAt={}", locationId, locationEntity.getDeletedAt());
        return locationMapper.toResponse(locationEntity);
    }

    @Override
    @Cacheable(value = "branch_active_validation", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #locationId")
    @Transactional(readOnly = true)
    public UUID validateLocationActive(UUID locationId) {
        Location location = locationRepository.findById(locationId).orElseThrow(LocationNotFoundException::new);
        if (location.getDeletedAt() != null) throw new LocationDisabledException();
        return locationId;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getActiveLocationIds() {
        return locationRepository.findActiveIds();
    }
}
