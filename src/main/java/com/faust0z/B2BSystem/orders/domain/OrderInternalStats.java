package com.faust0z.B2BSystem.orders.domain;

import java.math.BigDecimal;

public record OrderInternalStats(
        long countThisMonth,
        BigDecimal revenueThisMonth
) {
}
