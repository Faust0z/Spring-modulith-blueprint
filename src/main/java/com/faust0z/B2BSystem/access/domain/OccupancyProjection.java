package com.faust0z.B2BSystem.access.domain;

import java.util.UUID;

public record OccupancyProjection(
        UUID locationId,
        Long total
) {
}
