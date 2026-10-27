package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CatalogTemplateUpdateRequest(
        @Schema(description = "Nombre", example = "Rutina Principiantes 2")
        @Size(min = 2, max = 50, message = "El nombre debe contener entre 2 y 50 caracteres")
        String name,

        @Schema(description = "Ejercicios de la rutina. Se guarda en un String con formato, por lo que se conservan emojis y espaciado")
        @Size(max = 10000, message = "El contenido no debe exceder los 10000 caracteres")
        String content,

        @Schema(description = "Categoría de la rutina", example = "PRINCIPIANTE")
        TemplateCategory category
) {
}
