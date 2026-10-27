package com.faust0z.B2BSystem.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Service plan update request")
public record ServicePlanUpdateRequest(
        @Schema(description = "Service plan name", example = "Monthly Plan")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Schema(description = "Duration in days", example = "30")
        @Positive(message = "Duration must be positive")
        Integer durationDays,

        @Schema(description = "Plan price", example = "45000.00")
        @Positive(message = "Price must be positive")
        BigDecimal price
) {
}
