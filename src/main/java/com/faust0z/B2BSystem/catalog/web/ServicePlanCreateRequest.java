package com.faust0z.B2BSystem.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "New service plan request")
public record ServicePlanCreateRequest(
        @Schema(description = "Service plan name", example = "Monthly Plan")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Schema(description = "Duration in days", example = "30")
        @Positive(message = "Duration must be positive")
        @NotNull(message = "Duration is required")
        Integer durationDays,

        @Schema(description = "Plan price", example = "45000.00")
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        BigDecimal price
) {
}
