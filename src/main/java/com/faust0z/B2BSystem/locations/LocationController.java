package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.RequirePinAuthorization;
import com.faust0z.B2BSystem.locations.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.locations.LocationResponse;
import com.faust0z.B2BSystem.locations.LocationService;
import com.faust0z.B2BSystem.locations.Location;
import com.faust0z.B2BSystem.locations.LocationController;
import com.faust0z.B2BSystem.locations.LocationCreateRequest;
import com.faust0z.B2BSystem.locations.LocationUpdateRequest;
@RestController
@RequestMapping("/v1/locations")
@Tag(name = "Locations", description = "Location management")
public class LocationController {

    private final LocationService locationService;

    LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "Get all locations")
    @ApiResponse(responseCode = "200", description = "Locations retrieved successfully")
    @GetMapping
    List<LocationResponse> getLocations() {
        return locationService.getLocations();
    }

    @Operation(summary = "Create a new location")
    @ApiResponse(responseCode = "400", description = "Location name already exists")
    @RequirePinAuthorization
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LocationResponse createLocation(@Valid @RequestBody LocationCreateRequest request) {
        return locationService.createLocation(request);
    }

    @Operation(summary = "Patch the location's data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @RequirePinAuthorization
    @PatchMapping("/{locationId}")
    LocationResponse updateLocation(@PathVariable UUID locationId, @Valid @RequestBody LocationUpdateRequest request) {
        return locationService.updateLocation(locationId, request);
    }

    @Operation(summary = "Disable a location (Soft Delete)")
    @ApiResponse(responseCode = "200", description = "Location status updated successfully")
    @RequirePinAuthorization
    @PatchMapping("/{locationId}/status")
    LocationResponse toggleLocationStatus(@PathVariable UUID locationId) {
        return locationService.toggleLocationStatus(locationId);
    }
}
