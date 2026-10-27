package com.faust0z.B2BSystem.access.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Access scan request")
public record ScanRequest(
        @Schema(description = "Identifier (DNI or access token)", example = "22097162")
        @NotBlank(message = "Identifier is required")
        @Pattern(regexp = "^\\d{7,9}$|^[0-9a-fA-F-]{36}$", message = "Invalid identifier format")
        String identifier,

        @Schema(description = "Location ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        @NotNull(message = "Location ID is required")
        UUID locationId
) {
}
