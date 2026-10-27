package com.faust0z.B2BSystem.contacts.infrastructure;

import com.faust0z.B2BSystem.contacts.domain.ContactDetailsView;
import com.faust0z.B2BSystem.contacts.domain.ContactQuickStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ContactDetailsViewRepository extends JpaRepository<ContactDetailsView, UUID>, JpaSpecificationExecutor<ContactDetailsView> {
    <T> Optional<T> findById(UUID id, Class<T> type);

    @Query("""
                SELECT new com.faust0z.B2BSystem.contacts.ContactQuickStatsProjection(
                    COUNT(m),
                    SUM(CASE WHEN m.accessUntil >= :now AND m.accessUntil <= :expiringCutoff THEN 1L ELSE 0L END),
                    SUM(CASE WHEN m.debtAmount > 0 THEN 1L ELSE 0L END),
                    SUM(CASE WHEN m.lastScannedAt <= :sevenDaysAgo OR m.lastScannedAt IS NULL THEN 1L ELSE 0L END)
                )
                FROM ContactDetailsView m
            """)
    ContactQuickStatsProjection getQuickStats(@Param("now") Instant now,
                                             @Param("expiringCutoff") Instant expiringCutoff,
                                             @Param("sevenDaysAgo") Instant sevenDaysAgo);
}
