package com.faust0z.B2BSystem.access.infrastructure;

import com.faust0z.B2BSystem.access.domain.AccessLogEntity;
import com.faust0z.B2BSystem.access.domain.AccessStatsProjection;
import com.faust0z.B2BSystem.access.domain.DailyCountProjection;
import com.faust0z.B2BSystem.access.domain.HourlyCountProjection;
import com.faust0z.B2BSystem.access.domain.OccupancyProjection;
import com.faust0z.B2BSystem.access.domain.AccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLogEntity, UUID>, JpaSpecificationExecutor<AccessLogEntity> {
    @Query("""
                SELECT new com.faust0z.B2BSystem.access.domain.AccessStatsProjection(
                    e.locationId,
                    SUM(CASE WHEN e.scannedAt >= :startOfDay THEN 1L ELSE 0L END),
                    SUM(CASE WHEN e.scannedAt >= :startOfWeek THEN 1L ELSE 0L END),
                    SUM(CASE WHEN e.scannedAt >= :startOfMonth THEN 1L ELSE 0L END)
                )
                FROM AccessLogEntity e
                WHERE e.locationId IN :locationIds AND e.scannedAt >= :startOfMonth AND e.scannedAt < :endOfMonth
                GROUP BY e.locationId
            """)
    List<AccessStatsProjection> getDashboardStats(@Param("locationIds") Set<UUID> locationIds, @Param("startOfDay") Instant startOfDay,
                                                  @Param("startOfWeek") Instant startOfWeek, @Param("startOfMonth") Instant startOfMonth,
                                                  @Param("endOfMonth") Instant endOfMonth);

    @Query(value = """
            SELECT e.location_id as locationId, CAST(e.scanned_at AT TIME ZONE :tz AS date) as entryDate, COUNT(*) as total
            FROM access_logs e
            WHERE e.tenant_id = :tenantId AND e.location_id IN :locationIds AND e.scanned_at >= :from AND e.scanned_at < :to
            GROUP BY e.location_id, entryDate
            ORDER BY entryDate ASC
            """, nativeQuery = true)
    List<DailyCountProjection> findDailyStats(@Param("tenantId") UUID tenantId, @Param("locationIds") Set<UUID> locationIds, @Param("from") Instant from, @Param("to") Instant to, @Param("tz") String tz);

    @Query(value = """
            SELECT e.location_id as locationId, CAST(EXTRACT(HOUR FROM e.scanned_at AT TIME ZONE :tz) AS INTEGER) as hour, CAST(COUNT(*) AS BIGINT) as total
            FROM access_logs e
            WHERE e.tenant_id = :tenantId AND e.location_id IN :locationIds AND e.scanned_at >= :from AND e.scanned_at < :to
            GROUP BY e.location_id, hour
            """, nativeQuery = true)
    List<HourlyCountProjection> findHourlyStats(@Param("tenantId") UUID tenantId, @Param("locationIds") Set<UUID> locationIds, @Param("from") Instant from, @Param("to") Instant to, @Param("tz") String tz);

    @Query("""
            SELECT e.locationId as locationId, COUNT(DISTINCT e.contactId) as total
            FROM AccessLogEntity e
            WHERE e.locationId IN :locationIds AND e.status = :status AND e.scannedAt > :from
            GROUP BY e.locationId
            """)
    List<OccupancyProjection> getCurrentOccupationEstimate(@Param("locationIds") Set<UUID> locationIds, @Param("status") AccessStatus status, @Param("from") Instant from);
}
