package com.faust0z.B2BSystem.access.domain;

import java.util.UUID;

public record OccupancyDetail(
        UUID id,
        String name,
        int activePeople
) {
}
