package com.faust0z.B2BSystem.catalog;

import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record CatalogTemplateResponse(
        @Schema(description = "ID de la rutina", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "Nombre de la rutina", example = "Rutina piernas")
        String name,

        @Schema(description = "Contenido de la rutina")
        String content,

        @Schema(description = "Deletion timestamp (null if active)")
        Instant deletedAt,

        @Schema(description = "Categoría de la rutina", example = "PRINCIPIANTE")
        TemplateCategory category
) {
    public boolean isActive() {
        return deletedAt == null;
    }
}
