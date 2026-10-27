package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.staffing.domain.StaffIdentifierType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Shift process request")
public record ShiftProcessRequest(
        @NotNull(message = "Identifier type is required")
        StaffIdentifierType type,

        @NotBlank(message = "Identifier is required")
        String identifier,

        @Schema(description = "Location ID")
        @NotNull(message = "Location ID is required")
        UUID locationId
) {
}
