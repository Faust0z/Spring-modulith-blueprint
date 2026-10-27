package com.faust0z.B2BSystem.locations;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.locations.LocationResponse;
public interface LocationInternalApi {
    UUID validateLocationActive(UUID locationId);

    Set<UUID> getActiveLocationIds();

    List<LocationResponse> getLocations();

    LocationResponse createDefaultLocation();
}
