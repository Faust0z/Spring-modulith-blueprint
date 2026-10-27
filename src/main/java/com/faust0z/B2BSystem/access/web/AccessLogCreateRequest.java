package com.faust0z.B2BSystem.access.web;

import com.faust0z.B2BSystem.access.domain.ScanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccessLogCreateRequest(
        @Schema(description = "The type of identifier the contact used to identify themselves: DNI or QR")
        @NotNull(message = "The scan type is required.")
        ScanType scanType,

        @Schema(description = "The contact's identifier")
        @NotBlank(message = "The identifier is required.")
        String contactIdentifier,

        @Schema(description = "Id of the location where the scan was performed")
        @NotNull(message = "The location id is required")
        UUID locationId
) {
}
