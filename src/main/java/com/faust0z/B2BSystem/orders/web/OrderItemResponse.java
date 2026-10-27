package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.domain.OrderItemType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Order item response")
public record OrderItemResponse(
        @Schema(description = "Item ID")
        UUID id,

        @Schema(description = "Item type", example = "SERVICE_PLAN")
        OrderItemType itemType,

        @Schema(description = "Service plan ID")
        UUID servicePlanId,

        @Schema(description = "Product ID")
        UUID productId,

        @Schema(description = "Item name", example = "Plan Mensual")
        String name,

        @Schema(description = "Quantity", example = "1")
        int quantity,

        @Schema(description = "Unit price", example = "1000.00")
        BigDecimal unitPrice,

        @Schema(description = "Total price", example = "1000.00")
        BigDecimal totalPrice
) {
}
