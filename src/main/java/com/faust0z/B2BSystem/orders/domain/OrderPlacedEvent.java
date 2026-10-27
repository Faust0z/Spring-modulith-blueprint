package com.faust0z.B2BSystem.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID contactId,
        BigDecimal totalAmount,
        Instant paidAt
) {
}
