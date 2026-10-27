package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.application.BillingDemoCycleService;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingDemoCycleService {

    private final BillingCycleRepository billingCycleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    // To make sure this uses the new TenantContext with the newly created tenantId
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDemoCycle(UUID tenantId, UUID planId) {
        SubscriptionPlanEntity plan = subscriptionPlanRepository.findById(planId).orElseThrow(SubscriptionPlanNotFoundException::new);
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new SubscriptionPlanNotFoundException();
        }

        LocalDate todayDate = Instant.now().atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDate();
        Instant periodStart = todayDate.atStartOfDay(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();
        Instant periodEnd = todayDate.plusDays(7)
                .atTime(23, 59, 59)
                .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .toInstant();

        BillingCycleEntity demoCycle = new BillingCycleEntity();
        demoCycle.setTenantId(tenantId);
        demoCycle.setPlanId(planId);
        demoCycle.setPeriodStart(periodStart);
        demoCycle.setPeriodEnd(periodEnd);
        demoCycle.setStatus(CycleStatus.PAID);
        demoCycle.setDemo(true);

        billingCycleRepository.save(demoCycle);
        log.info("Demo cycle created | tenantId={} | planId={}", tenantId, planId);
    }
}
