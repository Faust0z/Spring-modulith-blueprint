package com.faust0z.B2BSystem.orders.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Invoice creation request")
public record InvoiceCreateRequest(
        @Schema(description = "Order ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @Schema(description = "Due date for the invoice")
        Instant dueDate
) {
}
