package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.domain.OrderItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Individual order item")
public record OrderItemRequest(
        @Schema(description = "Type of item", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Item type is required")
        OrderItemType itemType,

        @Schema(description = "Service plan ID (required if itemType is SERVICE_PLAN)")
        UUID servicePlanId,

        @Schema(description = "Product ID (required if itemType is PRODUCT)")
        UUID productId,

        @Schema(description = "Quantity", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "Quantity must be greater than zero")
        int quantity,

        @Schema(description = "Unit price", example = "1000.00")
        BigDecimal unitPrice
) {
}
