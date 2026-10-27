package com.faust0z.B2BSystem.access.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Access log search filter")
public record AccessLogFilterRequest(
        @Schema(description = "Filter by member DNI, name, last name or membership name", example = "22097162")
        String searchTerm,

        @Schema(description = "Start date of the scan range")
        Instant scannedAtBegin,

        @Schema(description = "End date of the scan range")
        Instant scannedAtEnd,

        @Schema(description = "Id of the location where the scan was performed")
        UUID locationId
) {
}
