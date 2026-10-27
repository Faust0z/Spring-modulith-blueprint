package com.faust0z.B2BSystem.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Product update request")
public record ProductUpdateRequest(
        @Schema(description = "Product name", example = "Water Bottle")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Schema(description = "Product price", example = "3000.00")
        @Positive(message = "Price must be positive")
        BigDecimal price
) {
}
