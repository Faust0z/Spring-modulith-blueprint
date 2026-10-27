package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Order filter request")
public record OrderFilterRequest(
        @Schema(description = "Contact ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID contactId,

        @Schema(description = "Location ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID locationId,

        @Schema(description = "Payment method")
        PaymentMethod paymentMethod,

        @Schema(description = "Filter by voided status", example = "false")
        Boolean isVoided,

        @Schema(description = "Start date of paidAt range")
        Instant paidAtBegin,

        @Schema(description = "End date of paidAt range")
        Instant paidAtEnd
) {
}
