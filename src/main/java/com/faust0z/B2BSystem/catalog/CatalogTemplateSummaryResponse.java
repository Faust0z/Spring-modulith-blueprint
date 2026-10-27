package com.faust0z.B2BSystem.catalog;

import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record CatalogTemplateSummaryResponse(
        @Schema(description = "ID de la rutina", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "Nombre de la rutina", example = "Rutina 1")
        String name,

        @Schema(description = "Indica si la rutina está activo o desactivado", example = "true")
        boolean isActive,

        @Schema(description = "Categoría de la rutina", example = "PRINCIPIANTE")
        TemplateCategory category
) {
}
