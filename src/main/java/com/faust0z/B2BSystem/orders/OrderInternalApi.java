package com.faust0z.B2BSystem.orders;

import com.faust0z.B2BSystem.orders.domain.OrderInternalStats;
import com.faust0z.B2BSystem.orders.domain.TopOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface OrderInternalApi {
    Map<UUID, OrderInternalStats> getOrderStats(Set<UUID> locationIds, Instant from, Instant to);

    Map<UUID, List<TopOrder>> getTopOrders(Set<UUID> locationIds, Instant from, Instant to);
}
