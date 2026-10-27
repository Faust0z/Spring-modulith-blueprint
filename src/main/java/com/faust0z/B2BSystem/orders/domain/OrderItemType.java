package com.faust0z.B2BSystem.orders.domain;

import java.math.BigDecimal;

public record OrderItemType(
        String name
) {
    public static final OrderItemType SERVICE_PLAN = new OrderItemType("SERVICE_PLAN");
    public static final OrderItemType PRODUCT = new OrderItemType("PRODUCT");
}
