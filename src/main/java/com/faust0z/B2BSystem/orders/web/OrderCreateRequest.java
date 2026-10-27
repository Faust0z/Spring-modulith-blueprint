package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "New order request")
public record OrderCreateRequest(
        @Schema(description = "Contact ID (buyer)", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID contactId,

        @Schema(description = "Location ID where the order was placed", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        @NotNull(message = "Location ID is required")
        UUID locationId,

        @Schema(description = "Payment method", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {
}
