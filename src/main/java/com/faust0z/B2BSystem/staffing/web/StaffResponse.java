package com.faust0z.B2BSystem.staffing.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Staff response")
public record StaffResponse(
        @Schema(description = "Staff ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Turner")
        String lastName,

        @Schema(description = "DNI", example = "22097162")
        String dni,

        @Schema(description = "Deletion timestamp (null if active)")
        Instant deletedAt
) {
    public boolean isActive() {
        return deletedAt == null;
    }
}
