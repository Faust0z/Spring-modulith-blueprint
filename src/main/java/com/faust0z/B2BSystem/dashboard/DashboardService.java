package com.faust0z.B2BSystem.dashboard;

import com.faust0z.B2BSystem.access.AccessLogInternalApi;
import com.faust0z.B2BSystem.access.domain.AccessInternalStats;
import com.faust0z.B2BSystem.access.web.AccessLogOccupancyResponse;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.contacts.domain.ContactInternalStats;
import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.orders.OrderInternalApi;
import com.faust0z.B2BSystem.orders.domain.OrderInternalStats;
import com.faust0z.B2BSystem.orders.domain.TopOrder;
import com.faust0z.B2BSystem.dashboard.DashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContactInternalApi contactInternalApi;
    private final AccessLogInternalApi accessLogInternalApi;
    private final OrderInternalApi orderInternalApi;
    private final LocationInternalApi locationInternalApi;

    @Cacheable(
            value = "dashboard",
            key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + '_' + #start + '_' + #end + '_' + #minutes"
    )
    public DashboardResponse compileDashboardData(LocalDate start, LocalDate end, int minutes) {
        log.debug("Compiling dashboard data | start={} | end={} | minutes={}", start, end, minutes);

        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        ZonedDateTime now = ZonedDateTime.now(zone);

        Instant startOfDay = start.atStartOfDay(zone).toInstant();
        Instant startOfWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant();
        Instant startOfMonth = start.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zone).toInstant();
        Instant endOfMonth = start.with(TemporalAdjusters.firstDayOfMonth()).plusMonths(1).atStartOfDay(zone).toInstant();
        Set<UUID> activeLocations = locationInternalApi.getActiveLocationIds();

        LocalDate today = LocalDate.now(zone);
        boolean liveView = !end.isBefore(today) && !start.isAfter(today);

        ContactInternalStats contactStats = liveView
                ? contactInternalApi.getContactStats(now.toInstant())
                : new ContactInternalStats(0, 0, 0);
        Map<UUID, AccessInternalStats> accessStats = accessLogInternalApi.getAccessStats(activeLocations, startOfDay, startOfWeek, startOfMonth, endOfMonth);
        Map<UUID, OrderInternalStats> orderStats = orderInternalApi.getOrderStats(activeLocations, startOfMonth, endOfMonth);
        Map<UUID, List<TopOrder>> topOrders = orderInternalApi.getTopOrders(activeLocations, startOfMonth, endOfMonth);

        Map<UUID, Map<LocalDate, Long>> dailyAccess = accessLogInternalApi.getDailyAccessStats(start, end);
        Map<UUID, Map<Integer, Long>> hourlyAccess = accessLogInternalApi.getHourlyAccessStats(start, end);
        AccessLogOccupancyResponse occupancy = liveView
                ? accessLogInternalApi.getCurrentOccupancy(minutes)
                : new AccessLogOccupancyResponse(List.of());

        log.debug("Compiled dashboard data");
        return new DashboardResponse(contactStats, accessStats, orderStats, dailyAccess, hourlyAccess,
                occupancy, topOrders);
    }
}
