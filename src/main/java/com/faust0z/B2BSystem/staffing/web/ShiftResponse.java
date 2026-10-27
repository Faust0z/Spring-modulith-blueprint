package com.faust0z.B2BSystem.staffing.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shift response")
public record ShiftResponse(
        @Schema(description = "Shift ID")
        UUID id,

        @Schema(description = "Staff ID")
        UUID staffId,

        @Schema(description = "Staff full name")
        String fullName,

        @Schema(description = "Clock in time")
        Instant clockInAt,

        @Schema(description = "Clock out time")
        Instant clockOutAt,

        @Schema(description = "Whether the shift was auto-closed")
        boolean autoClosed,

        @Schema(description = "Whether the shift is active")
        boolean isActive,

        @Schema(description = "Location ID")
        UUID locationId
) {
}
