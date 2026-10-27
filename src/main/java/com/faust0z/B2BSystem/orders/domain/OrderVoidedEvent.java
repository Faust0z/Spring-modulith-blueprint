package com.faust0z.B2BSystem.orders.domain;

import java.util.UUID;

public record OrderVoidedEvent(
        UUID orderId,
        UUID contactId
) {
}
