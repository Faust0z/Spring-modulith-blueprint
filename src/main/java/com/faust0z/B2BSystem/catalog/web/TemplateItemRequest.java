package com.faust0z.B2BSystem.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "Detalle de producto individual en la venta")
public record TemplateItemRequest(
        @Schema(description = "ID del producto en el catálogo", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El id del producto es obligatorio")
        UUID productId,

        @Schema(description = "Cantidad de unidades (mínimo 1)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "La cantidad debe ser mayor a cero")
        int quantity
) {
}
