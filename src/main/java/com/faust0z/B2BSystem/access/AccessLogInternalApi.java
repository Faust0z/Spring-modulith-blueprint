package com.faust0z.B2BSystem.access;

import com.faust0z.B2BSystem.access.web.AccessLogCreateRequest;
import com.faust0z.B2BSystem.access.web.AccessLogOccupancyResponse;
import com.faust0z.B2BSystem.access.web.AccessLogResponse;
import com.faust0z.B2BSystem.access.domain.AccessInternalStats;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AccessLogInternalApi {
    Map<UUID, AccessInternalStats> getAccessStats(Set<UUID> locationIds, Instant startOfDay, Instant startOfWeek, Instant startOfMonth, Instant endOfMonth);

    AccessLogResponse createAccessLog(AccessLogCreateRequest request);

    Map<UUID, Map<LocalDate, Long>> getDailyAccessStats(LocalDate start, LocalDate end);

    Map<UUID, Map<Integer, Long>> getHourlyAccessStats(LocalDate start, LocalDate end);

    AccessLogOccupancyResponse getCurrentOccupancy(int minutes);
}
