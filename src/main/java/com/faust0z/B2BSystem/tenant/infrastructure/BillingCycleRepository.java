package com.faust0z.B2BSystem.tenant.infrastructure;

import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
public interface BillingCycleRepository extends JpaRepository<BillingCycleEntity, UUID>, JpaSpecificationExecutor<BillingCycleEntity> {

    <T> Page<T> findBy(Pageable pageable, Class<T> type);

    <T> Optional<T> findById(UUID id, Class<T> type);

    @Query("SELECT c FROM BillingCycleEntity c WHERE c.status IN ('PAID', 'PENDING_PAYMENT')")
    <T> Optional<T> findActive(Class<T> type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BillingCycleEntity c WHERE c.status IN ('PAID', 'PENDING_PAYMENT')")
    <T> Optional<T> findActiveForWrite(Class<T> type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BillingCycleEntity c WHERE c.id = :id")
    Optional<BillingCycleEntity> findByIdForWrite(@Param("id") UUID id);

    @Query(value = "SELECT c FROM BillingCycleEntity c WHERE c.status IN ('PAID', 'PENDING_PAYMENT', 'CANCELLED') " +
            "ORDER BY CASE c.status WHEN 'PENDING_PAYMENT' THEN 0 WHEN 'PAID' THEN 1 ELSE 2 END, c.createdAt DESC")
    <T> List<T> findPayableCycles(Class<T> type);

    @Query(value = "SELECT c.tenant_id FROM billing_cycles c WHERE c.status IN ('PAID', 'PENDING_PAYMENT') AND c.is_demo = false AND c.period_end < :now",
            nativeQuery = true)
    List<UUID> findTenantIdsWithExpiredActive(@Param("now") Instant now);

    @Query(value = "SELECT * FROM billing_cycles c WHERE c.status IN ('PAID', 'PENDING_PAYMENT') AND c.is_demo = false AND c.period_start <= :now AND c.period_end >= :now",
            nativeQuery = true)
    List<BillingCycleEntity> findActivesNative(@Param("now") Instant now);
}
