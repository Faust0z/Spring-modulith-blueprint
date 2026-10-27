package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.application.CycleRollEngine;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleRollEngine {

    private final BillingCycleRepository billingCycleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ContactInternalApi memberApi;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 30 0 * * *", zone = "America/Argentina/Buenos_Aires")
    public void recordNightlyPeaks() {
        Instant now = Instant.now();
        log.debug("Recording nightly peak members");
        List<BillingCycleEntity> activeCycles = billingCycleRepository.findActivesNative(now);

        for (BillingCycleEntity cycle : activeCycles) {
            try {
                TenantContext.setTenantId(cycle.getTenantId());

                transactionTemplate.executeWithoutResult(status -> {
                    int currentMembersWithAccess = memberApi.countContactsWithAccess(now);
                    if (currentMembersWithAccess > cycle.getPeakMembers()) {
                        log.info("Peak members updated | tenantId={} | billingCycleId={} | previousPeakMembers={} | newPeakMembers={}",
                                cycle.getTenantId(), cycle.getId(), cycle.getPeakMembers(), currentMembersWithAccess);
                        cycle.setPeakMembers(currentMembersWithAccess);
                        billingCycleRepository.save(cycle);
                    }
                });
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Argentina/Buenos_Aires")
    public void executeNightlyRoll() {
        List<UUID> expiredTenantIds = billingCycleRepository.findTenantIdsWithExpiredActive(Instant.now());

        for (UUID tenantId : expiredTenantIds) {
            try {
                TenantContext.setTenantId(tenantId);
                log.info("Rolling cycle | tenantId={}", tenantId);
                rollSingleTenant(tenantId);
            } finally {
                TenantContext.clear();
            }
        }
    }

    public void rollSingleTenant(UUID tenantId) {
        transactionTemplate.executeWithoutResult(status -> {
            BillingCycleEntity justExpiredCycle = billingCycleRepository.findActiveForWrite(BillingCycleEntity.class).orElseThrow();

            if (justExpiredCycle.getStatus() == CycleStatus.PENDING_PAYMENT) {
                justExpiredCycle.setStatus(CycleStatus.CANCELLED);
                log.info("Tenant didn't pay cycle and got cancelled | tenantId={} | cycleId={}", tenantId, justExpiredCycle.getId());
                return;
            }

            justExpiredCycle.setStatus(CycleStatus.COMPLETED);

            Instant now = Instant.now();
            Instant periodStart = justExpiredCycle.getPeriodEnd();
            if (ChronoUnit.DAYS.between(periodStart, now) >= 30) periodStart = now;

            BillingCycleEntity newCycle = new BillingCycleEntity();
            newCycle.setTenantId(tenantId);
            newCycle.setPeriodStart(periodStart);
            newCycle.setPeriodEnd(calculateBuenosAiresEndOfDay(periodStart.plus(30, ChronoUnit.DAYS)));

            int discount = 0;
            if (justExpiredCycle.getRemainingDiscountMonths() > 0) {
                discount = justExpiredCycle.getDiscountPercentage();
                newCycle.setRemainingDiscountMonths(justExpiredCycle.getRemainingDiscountMonths() - 1);
            }
            newCycle.setDiscountPercentage(discount);

            UUID basePlanId = justExpiredCycle.getNextPlanId() != null
                    ? justExpiredCycle.getNextPlanId()
                    : justExpiredCycle.getPlanId();
            SubscriptionPlanEntity currentPlan = subscriptionPlanRepository.findById(basePlanId).orElseThrow();
            SubscriptionPlanEntity targetPlan = resolvePlanForNextCycle(justExpiredCycle, currentPlan);
            newCycle.setPlanId(targetPlan.getId());
            newCycle.setStatus(CycleStatus.PENDING_PAYMENT);
            newCycle.setAmount((int) (targetPlan.getPrice() * ((100 - discount) / 100.0)));

            billingCycleRepository.save(newCycle);

            log.info("Cycle rolled | tenantId={} | planId={} | periodStart={} | periodEnd={} | amount={}",
                    tenantId, targetPlan.getId(), newCycle.getPeriodStart(), newCycle.getPeriodEnd(), newCycle.getAmount());
        });
    }

    private SubscriptionPlanEntity resolvePlanForNextCycle(BillingCycleEntity expiredCycle, SubscriptionPlanEntity currentPlan) {
        if ("LEGADO".equalsIgnoreCase(currentPlan.getTier())) return currentPlan;

        int peakMembers = expiredCycle.getPeakMembers();
        if (currentPlan.getMaxMembers() == null || peakMembers <= currentPlan.getMaxMembers()) return currentPlan;

        Comparator<SubscriptionPlanEntity> byLimit = Comparator.comparingInt(
                (SubscriptionPlanEntity p) -> p.getMaxMembers() == null ? Integer.MAX_VALUE : p.getMaxMembers());
        List<SubscriptionPlanEntity> candidates = subscriptionPlanRepository.findByIsActiveTrue().stream()
                .filter(p -> !"LEGADO".equalsIgnoreCase(p.getTier()))
                .sorted(byLimit.thenComparingInt(SubscriptionPlanEntity::getPrice))
                .toList();

        return candidates.stream()
                .filter(p -> p.getMaxMembers() != null && p.getMaxMembers() >= peakMembers)
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(p -> p.getMaxMembers() == null)
                        .findFirst()
                        .orElse(currentPlan));
    }

    private Instant calculateBuenosAiresEndOfDay(Instant instant) {
        return instant.atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .toLocalDate()
                .atTime(23, 59, 59)
                .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .toInstant();
    }
}
