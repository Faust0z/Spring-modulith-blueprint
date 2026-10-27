package com.faust0z.B2BSystem.access.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

record AccessLogResponse(
        @Schema(description = "Access log record ID")
        UUID id,

        @Schema(description = "Contact ID")
        UUID contactId,

        @Schema(description = "Member full name")
        String memberName,

        @Schema(description = "Membership expiration date", example = "2026-12-31T23:59:59Z")
        Instant accessUntil,

        @Schema(description = "Scan date and time")
        Instant scannedAt,

        @Schema(description = "Access status", example = "ALLOWED")
        AccessStatus status,

        @Schema(description = "Member debt amount", example = "1000")
        Integer memberDebtAmount,

        @Schema(description = "Id of the location where the scan was performed")
        UUID locationId
) {
}
