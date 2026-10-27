package com.faust0z.B2BSystem.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Product response")
public record ProductResponse(
        @Schema(description = "Product ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "Product name", example = "Water Bottle")
        String name,

        @Schema(description = "Product price", example = "3000.00")
        BigDecimal price,

        @Schema(description = "Deletion timestamp (null if active)")
        Instant deletedAt
) {
    public boolean isActive() {
        return deletedAt == null;
    }
}
