package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        UUID orderId,
        String invoiceNumber,
        Instant issueDate,
        Instant dueDate,
        BigDecimal totalAmount,
        InvoiceStatus status,
        Instant createdAt
) {
}
