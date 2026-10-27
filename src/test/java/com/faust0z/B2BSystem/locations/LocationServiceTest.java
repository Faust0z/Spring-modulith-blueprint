package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.locations.LocationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.locations.LocationMapper;
import com.faust0z.B2BSystem.locations.LocationService;
import com.faust0z.B2BSystem.locations.Location;
import com.faust0z.B2BSystem.locations.LocationRepository;
import com.faust0z.B2BSystem.locations.LocationCreateRequest;
import com.faust0z.B2BSystem.locations.LocationDisabledException;
import com.faust0z.B2BSystem.locations.LocationNameExistsException;
import com.faust0z.B2BSystem.locations.LocationNotFoundException;
import com.faust0z.B2BSystem.locations.LocationUpdateRequest;
import java.time.Instant;
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    private Location createLocation(UUID id, String name, Instant deletedAt) {
        Location entity = new Location();
        entity.setId(id);
        entity.setName(name);
        entity.setDeletedAt(deletedAt);
        return entity;
    }

    private LocationResponse createLocationResponse(UUID id, String name, Instant deletedAt) {
        return new LocationResponse(id, name, deletedAt);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    class GetLocations {

        @Test
        void getLocations_Execution_ReturnsMappedResponseList() {
            LocationResponse response = createLocationResponse(UUID.randomUUID(), "Sede Central", null);
            when(locationRepository.findAllBy(LocationResponse.class)).thenReturn(List.of(response));

            List<LocationResponse> result = locationService.getLocations();

            assertThat(result).hasSize(1).containsExactly(response);
            verify(locationRepository).findAllBy(LocationResponse.class);
        }
    }

    @Nested
    class CreateLocation {

        @Test
        void createLocation_NameAlreadyExists_ThrowsLocationNameExistsException() {
            LocationCreateRequest request = new LocationCreateRequest("Sede Laprida");
            when(locationRepository.existsByNameIgnoreCase(request.name())).thenReturn(true);

            assertThatThrownBy(() -> locationService.createLocation(request))
                    .isInstanceOf(LocationNameExistsException.class);

            verify(locationRepository, never()).saveAndFlush(any());
        }

        @Test
        void createLocation_ValidRequest_SavesAndReturnsResponse() {
            UUID id = UUID.randomUUID();
            LocationCreateRequest request = new LocationCreateRequest("Sede Laprida");
            Location entity = createLocation(id, "Sede Laprida", null);
            LocationResponse response = createLocationResponse(id, "Sede Laprida", null);

            when(locationRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
            when(locationMapper.toEntity(request)).thenReturn(entity);
            when(locationRepository.saveAndFlush(entity)).thenReturn(entity);
            when(locationMapper.toResponse(entity)).thenReturn(response);

            LocationResponse result = locationService.createLocation(request);

            assertThat(result).isEqualTo(response);
            verify(locationRepository).saveAndFlush(entity);
        }
    }

    @Nested
    class UpdateLocation {

        @Test
        void updateLocation_LocationDoesNotExist_ThrowsLocationNotFoundException() {
            UUID id = UUID.randomUUID();
            LocationUpdateRequest request = new LocationUpdateRequest("Sede Maipú");
            when(locationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> locationService.updateLocation(id, request))
                    .isInstanceOf(LocationNotFoundException.class);

            verify(locationRepository, never()).save(any());
        }

        @Test
        void updateLocation_NameValidationFails_ThrowsLocationNameExistsException() {
            UUID id = UUID.randomUUID();
            LocationUpdateRequest request = new LocationUpdateRequest("Sede Maipú");
            Location existingLocation = createLocation(id, "Old Name", null);

            when(locationRepository.findById(id)).thenReturn(Optional.of(existingLocation));
            when(locationRepository.existsByNameIgnoreCase(existingLocation.getName())).thenReturn(false);

            assertThatThrownBy(() -> locationService.updateLocation(id, request))
                    .isInstanceOf(LocationNameExistsException.class);

            verify(locationRepository, never()).save(any());
        }

        @Test
        void updateLocation_ValidRequest_UpdatesAndReturnsResponse() {
            UUID id = UUID.randomUUID();
            LocationUpdateRequest request = new LocationUpdateRequest("Sede Maipú");
            Location existingLocation = createLocation(id, "Old Name", null);
            LocationResponse response = createLocationResponse(id, "Sede Maipú", null);

            when(locationRepository.findById(id)).thenReturn(Optional.of(existingLocation));
            when(locationRepository.existsByNameIgnoreCase(existingLocation.getName())).thenReturn(true);
            when(locationMapper.toResponse(existingLocation)).thenReturn(response);

            LocationResponse result = locationService.updateLocation(id, request);

            assertThat(result).isEqualTo(response);
            verify(locationMapper).updateFromRequest(request, existingLocation);
            verify(locationRepository).save(existingLocation);
        }
    }

    @Nested
    class ToggleLocationStatus {

        @Test
        void toggleLocationStatus_LocationDoesNotExist_ThrowsLocationNotFoundException() {
            UUID id = UUID.randomUUID();
            when(locationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> locationService.toggleLocationStatus(id))
                    .isInstanceOf(LocationNotFoundException.class);
        }

        @Test
        void toggleLocationStatus_LocationActive_InvertsToInactiveAndReturnsResponse() {
            UUID id = UUID.randomUUID();
            Location entity = createLocation(id, "Sede Norte", null);
            LocationResponse response = createLocationResponse(id, "Sede Norte", Instant.now());

            when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
            when(locationMapper.toResponse(entity)).thenReturn(response);

            LocationResponse result = locationService.toggleLocationStatus(id);

            assertThat(entity.getDeletedAt()).isNotNull();
            assertThat(result).isEqualTo(response);
        }
    }

    @Nested
    class InternalApiOperations {

        @Test
        void validateLocationActive_LocationActive_ReturnsLocationId() {
            UUID id = UUID.randomUUID();
            Location location = new Location();
            location.setId(id);
            location.setDeletedAt(null);
            when(locationRepository.findById(eq(id))).thenReturn(Optional.of(location));

            UUID result = locationService.validateLocationActive(id);

            assertThat(result).isEqualTo(id);
        }

        @Test
        void validateLocationActive_LocationInactive_ThrowsLocationDisabledException() {
            UUID id = UUID.randomUUID();
            Location location = new Location();
            location.setId(id);
            location.setDeletedAt(Instant.now());
            when(locationRepository.findById(eq(id))).thenReturn(Optional.of(location));

            assertThatThrownBy(() -> locationService.validateLocationActive(id))
                    .isInstanceOf(LocationDisabledException.class);
        }

        @Test
        void validateLocationActive_LocationNotFound_ThrowsLocationNotFoundException() {
            UUID id = UUID.randomUUID();
            when(locationRepository.findById(eq(id))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> locationService.validateLocationActive(id))
                    .isInstanceOf(LocationNotFoundException.class);
        }

        @Test
        void getActiveLocationIds_Execution_ReturnsRepositorySet() {
            Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
            when(locationRepository.findActiveIds()).thenReturn(ids);

            Set<UUID> result = locationService.getActiveLocationIds();

            assertThat(result).isEqualTo(ids);
            verify(locationRepository).findActiveIds();
        }
    }
}

