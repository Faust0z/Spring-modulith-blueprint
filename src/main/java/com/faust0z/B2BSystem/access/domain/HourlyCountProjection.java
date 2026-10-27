package com.faust0z.B2BSystem.access.domain;

import java.util.UUID;

public record HourlyCountProjection(
        UUID locationId,
        Integer hour,
        Long total
) {
}
