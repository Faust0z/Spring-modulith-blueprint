package com.faust0z.B2BSystem.locations;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "DTO de sedes")
public record LocationResponse(
        @Schema(description = "ID de la sede", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "Nombre", example = "Sede Laprida")
        String name,

        @Schema(description = "Deletion timestamp (null if active)")
        Instant deletedAt
) {
    public boolean isActive() {
        return deletedAt == null;
    }
}
