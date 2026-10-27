package com.faust0z.B2BSystem.staffing.infrastructure;

import com.faust0z.B2BSystem.staffing.domain.ShiftEntity;
import com.faust0z.B2BSystem.staffing.domain.ShiftsHoursProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<ShiftEntity, UUID>, JpaSpecificationExecutor<ShiftEntity> {
    Optional<ShiftEntity> findByStaffIdAndClockOutAtIsNullAndIsActiveTrue(@Param("staffId") UUID staffId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE shifts " +
            "SET clock_out_at = clock_in_at + INTERVAL '18 hours', " +
            "    auto_closed = true, " +
            "    version = version + 1, " +
            "    last_modified_at = :now " +
            " WHERE clock_out_at IS NULL " +
            "  AND is_active = true " +
            "  AND clock_in_at < :threshold",
            nativeQuery = true)
    int closeUnfinishedShifts(@Param("threshold") Instant threshold, @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT
                w.staff_id as staff_id,
                SUM(EXTRACT(EPOCH FROM (w.clock_out_at - w.clock_in_at)) / 3600) as totalHours
                FROM shifts w
                WHERE w.clock_in_at >= :start AND w.clock_in_at < :end
                AND w.tenant_id = :tenantId
                AND w.auto_closed = false
                AND w.clock_out_at IS NOT NULL
                GROUP BY w.staff_id
            """)
    List<ShiftsHoursProjection> findHoursGroupedByStaff(@Param("start") Instant start, @Param("end") Instant end, @Param("tenantId") UUID tenantId);
}
