package com.faust0z.B2BSystem.access.domain;

import java.util.UUID;

public record AccessStatsProjection(
        UUID locationId,
        Long today,
        Long thisWeek,
        Long thisMonth
) {
}
