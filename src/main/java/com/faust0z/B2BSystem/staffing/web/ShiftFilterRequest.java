package com.faust0z.B2BSystem.staffing.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shift filter request")
public record ShiftFilterRequest(
        @Schema(description = "Search by name or DNI", example = "Faust Mans")
        String searchTerm,

        @Schema(description = "Clock in start time")
        Instant clockInAt,

        @Schema(description = "Clock in end time")
        Instant clockOutAt,

        @Schema(description = "Filter by auto-closed status")
        Boolean autoClosed,

        @Schema(description = "Filter by active status", example = "false")
        Boolean isActive,

        @Schema(description = "Location ID")
        UUID locationId,

        @Schema(description = "Filter open (true) or closed (false) shifts", example = "true")
        Boolean isOpen
) {
}
