package com.faust0z.B2BSystem.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "New product request")
public record ProductCreateRequest(
        @Schema(description = "Product name", example = "Water Bottle")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Schema(description = "Product price", example = "3000.00")
        @Positive(message = "Price must be positive")
        @NotNull(message = "Price is required")
        BigDecimal price
) {
}
