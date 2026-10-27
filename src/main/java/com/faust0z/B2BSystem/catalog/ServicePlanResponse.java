package com.faust0z.B2BSystem.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Service plan response")
public record ServicePlanResponse(
        @Schema(description = "Service plan ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "Plan name", example = "Monthly Plan")
        String name,

        @Schema(description = "Duration in days", example = "30")
        Integer durationDays,

        @Schema(description = "Plan price", example = "45000.00")
        BigDecimal price,

        @Schema(description = "Deletion timestamp (null if active)")
        Instant deletedAt
) {
    public boolean isActive() {
        return deletedAt == null;
    }
}
