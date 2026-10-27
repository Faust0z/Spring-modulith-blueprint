package com.faust0z.B2BSystem.dashboard;

import com.faust0z.B2BSystem.access.domain.AccessInternalStats;
import com.faust0z.B2BSystem.access.web.AccessLogOccupancyResponse;
import com.faust0z.B2BSystem.contacts.domain.ContactInternalStats;
import com.faust0z.B2BSystem.orders.domain.OrderInternalStats;
import com.faust0z.B2BSystem.orders.domain.TopOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(description = "Dashboard compiled data")
public class DashboardResponse {

    @Schema(description = "Contact statistics")
    private ContactInternalStats contactStats;

    @Schema(description = "Access statistics by location")
    private Map<UUID, AccessInternalStats> accessStats;

    @Schema(description = "Order statistics by location")
    private Map<UUID, OrderInternalStats> orderStats;

    @Schema(description = "Daily access counts by location and date")
    private Map<UUID, Map<LocalDate, Long>> dailyAccess;

    @Schema(description = "Hourly access counts by location and hour")
    private Map<UUID, Map<Integer, Long>> hourlyAccess;

    @Schema(description = "Current occupancy estimate")
    private AccessLogOccupancyResponse occupancy;

    @Schema(description = "Top orders by location")
    private Map<UUID, List<TopOrder>> topOrders;
}
