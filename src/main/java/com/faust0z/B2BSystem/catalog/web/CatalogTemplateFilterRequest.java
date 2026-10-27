package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtro de rutinas")
public record CatalogTemplateFilterRequest(
        @Schema(description = "Nombre de la rutina")
        String name,

        @Schema(description = "Categoría de la rutina")
        TemplateCategory category
) {
}
