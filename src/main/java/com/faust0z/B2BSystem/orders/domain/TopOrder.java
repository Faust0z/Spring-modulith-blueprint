package com.faust0z.B2BSystem.orders.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TopOrder(
        UUID locationId,
        String locationName,
        BigDecimal totalAmount,
        long count
) {
}
