package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.tenant.BillingCycleHydratedResponse;
import com.faust0z.B2BSystem.tenant.BillingCycleInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleNotFoundException;
import com.faust0z.B2BSystem.tenant.domain.CycleAlreadyPaidException;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.PlanChangeAlreadyScheduledException;
import com.faust0z.B2BSystem.tenant.domain.PlanMemberLimitExceededException;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayFirstNonDemoRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.TenantChangePlanRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.BillingCycleHydratedResponse;
import com.faust0z.B2BSystem.tenant.BillingCycleInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleNotFoundException;
import com.faust0z.B2BSystem.tenant.domain.CycleAlreadyPaidException;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.PlanChangeAlreadyScheduledException;
import com.faust0z.B2BSystem.tenant.domain.PlanMemberLimitExceededException;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayFirstNonDemoRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.TenantChangePlanRequest;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingCycleService implements BillingCycleInternalApi {

    private final BillingCycleRepository billingCycleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ContactInternalApi memberApi;

    public Page<BillingCycleResponse> findAll(Pageable pageable) {
        log.debug("Querying billing cycles");
        return billingCycleRepository.findBy(pageable, BillingCycleResponse.class);
    }

    public BillingCycleResponse findById(UUID billingCycleId) {
        log.debug("Querying billing cycle | billingCycleId={}", billingCycleId);
        return billingCycleRepository.findById(billingCycleId, BillingCycleResponse.class).orElseThrow(BillingCycleNotFoundException::new);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean hasValidCycle() {
        log.debug("Checking valid cycle");

        BillingCycleResponse cycleResponse = billingCycleRepository.findActive(BillingCycleResponse.class)
                .orElseThrow(BillingCycleNotFoundException::new);

        if (cycleResponse.isDemo()) return Instant.now().isBefore(cycleResponse.periodEnd());

        final int GRACE_PERIOD_DAYS = 7;
        if (cycleResponse.status() == CycleStatus.PENDING_PAYMENT) {
            Instant graceEnd = cycleResponse.periodStart().truncatedTo(ChronoUnit.DAYS).plus(GRACE_PERIOD_DAYS + 1L, ChronoUnit.DAYS);
            return Instant.now().isBefore(graceEnd);
        }
        return Instant.now().isBefore(cycleResponse.periodEnd());
    }

    @Override
    public BillingCycleHydratedResponse computeNextCycleToPay(TenantInternalResponse tenant) {
        log.debug("Computing next cycle to pay | tenantId={}", tenant.id());

        BillingCycleResponse activeCycle = billingCycleRepository.findPayableCycles(BillingCycleResponse.class).stream()
                .findFirst()
                .orElseThrow(BillingCycleNotFoundException::new);

        SubscriptionPlanResponse plan = subscriptionPlanRepository.findById(activeCycle.planId(), SubscriptionPlanResponse.class).orElseThrow();

        if (activeCycle.status() == CycleStatus.CANCELLED) {
            Instant now = Instant.now();
            // If they didn't pay in 30+ days, we just simulate what their new cycles would look like if they began paying again today
            return new BillingCycleHydratedResponse(activeCycle.id(), tenant.id(), tenant.name(), activeCycle.planId(), plan.tier(),
                    now, now.plus(30, ChronoUnit.DAYS), 0, 0, plan.price(), CycleStatus.PENDING_PAYMENT, false);
        }

        return new BillingCycleHydratedResponse(activeCycle.id(), tenant.id(), tenant.name(), activeCycle.planId(), plan.tier(),
                activeCycle.periodStart(), activeCycle.periodEnd(), activeCycle.peakMembers(), activeCycle.discountPercentage(),
                activeCycle.amount(), activeCycle.status(), activeCycle.isDemo());
    }

    @Transactional
    public BillingCycleResponse payCycle(BillingCyclePayRequest request) {
        log.debug("Processing payment for cycle | tenantId={} | cycleId={}", request.tenantId(), request.cycleId());

        BillingCycleEntity cycle = billingCycleRepository.findByIdForWrite(request.cycleId()).orElseThrow(BillingCycleNotFoundException::new);

        if (cycle.getStatus() == CycleStatus.PAID) throw new CycleAlreadyPaidException();
        if (cycle.getStatus() != CycleStatus.PENDING_PAYMENT) throw new BillingCycleNotFoundException();

        cycle.setStatus(CycleStatus.PAID);
        if (request.optionalExtra() != null && request.optionalExtra() > 0) cycle.setAmount(cycle.getAmount() + request.optionalExtra());

        return toResponse(cycle);
    }

    @Transactional
    public BillingCycleResponse payFirstCycle(BillingCyclePayFirstNonDemoRequest request) {
        log.debug("Processing first payment | tenantId={} | planId={}", request.tenantId(), request.planId());

        Instant now = Instant.now();

        billingCycleRepository.findActiveForWrite(BillingCycleEntity.class)
                .filter(BillingCycleEntity::isDemo)
                .ifPresent(demoCycle -> {
                    demoCycle.setStatus(CycleStatus.COMPLETED);
                    if (demoCycle.getPeriodEnd() == null) demoCycle.setPeriodEnd(now);
                    billingCycleRepository.save(demoCycle);
                });

        SubscriptionPlanResponse plan = subscriptionPlanRepository.findById(request.planId(), SubscriptionPlanResponse.class)
                .orElseThrow(SubscriptionPlanNotFoundException::new);

        int discount = request.discountPercentage() != null ? request.discountPercentage() : 0;
        int finalAmount = request.amount() != null ? request.amount() : (int) (plan.price() * ((100 - discount) / 100.0));

        BillingCycleEntity newCycle = new BillingCycleEntity();
        newCycle.setTenantId(request.tenantId());
        newCycle.setPlanId(request.planId());
        newCycle.setPeriodStart(now);
        newCycle.setPeriodEnd(calculateBuenosAiresEndOfDay(now.plus(30, ChronoUnit.DAYS)));
        newCycle.setStatus(CycleStatus.PAID);
        newCycle.setDiscountPercentage(discount);
        newCycle.setRemainingDiscountMonths(request.remainingDiscountMonths() != null ? request.remainingDiscountMonths() : 0);
        newCycle.setAmount(finalAmount);
        newCycle.setDemo(false);

        billingCycleRepository.save(newCycle);

        return toResponse(newCycle);
    }

    @Transactional
    public BillingCycleResponse changePlan(UUID tenantId, TenantChangePlanRequest request) {
        log.debug("Changing tenant plan | tenantId={} | newPlanId={}", tenantId, request.planId());

        SubscriptionPlanEntity targetPlan = subscriptionPlanRepository.findById(request.planId()).orElseThrow(SubscriptionPlanNotFoundException::new);
        if (!Boolean.TRUE.equals(targetPlan.getIsActive()) || "LEGADO".equalsIgnoreCase(targetPlan.getTier())) {
            throw new SubscriptionPlanNotFoundException();
        }

        int currentMembers = memberApi.countContactsWithAccess(Instant.now());
        if (targetPlan.getMaxMembers() != null && currentMembers > targetPlan.getMaxMembers()) {
            throw new PlanMemberLimitExceededException();
        }

        BillingCycleEntity activeCycle = billingCycleRepository.findActiveForWrite(BillingCycleEntity.class).orElseThrow(BillingCycleNotFoundException::new);
        if (activeCycle.getNextPlanId() != null) throw new PlanChangeAlreadyScheduledException();

        activeCycle.setNextPlanId(targetPlan.getId());
        billingCycleRepository.save(activeCycle);

        log.info("Tenant plan change scheduled | tenantId={} | currentPlanId={} | nextPlanId={}", tenantId, activeCycle.getPlanId(), targetPlan.getId());
        return toResponse(activeCycle);
    }

    private BillingCycleResponse toResponse(BillingCycleEntity entity) {
        return new BillingCycleResponse(entity.getId(), entity.getTenantId(), entity.getPlanId(),
                entity.getNextPlanId(), entity.getPeriodStart(), entity.getPeriodEnd(), entity.getPeakMembers(),
                entity.getDiscountPercentage(), entity.getAmount(), entity.getStatus(), entity.isDemo());
    }

    private Instant calculateBuenosAiresEndOfDay(Instant instant) {
        return instant.atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .toLocalDate()
                .atTime(23, 59, 59)
                .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .toInstant();
    }
}
