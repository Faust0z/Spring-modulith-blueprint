package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Order response")
public record OrderResponse(
        @Schema(description = "Order ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "Contact ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID contactId,

        @Schema(description = "Location ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID locationId,

        @Schema(description = "Payment method", example = "EFECTIVO")
        PaymentMethod paymentMethod,

        @Schema(description = "Total amount", example = "40000.00")
        BigDecimal totalAmount,

        @Schema(description = "Order date", example = "2026-12-31T23:59:59Z")
        Instant paidAt,

        @Schema(description = "Order items")
        List<OrderItemResponse> items,

        @Schema(description = "Whether the order is voided", example = "false")
        Boolean isVoided
) {
}
