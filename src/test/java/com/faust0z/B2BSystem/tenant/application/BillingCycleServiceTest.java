package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.tenant.BillingCycleHydratedResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
@ExtendWith(MockitoExtension.class)
class BillingCycleServiceTest {

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private ContactInternalApi memberApi;

    @InjectMocks
    private BillingCycleService billingCycleService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private BillingCycleResponse createCycleResponse(Instant periodEnd, boolean isDemo, int amount, int peakMembers, UUID planId) {
        return new BillingCycleResponse(UUID.randomUUID(), UUID.randomUUID(), planId, null, periodEnd.minus(30, ChronoUnit.DAYS),
                periodEnd, peakMembers, 0, amount, CycleStatus.PAID, isDemo);
    }

    private BillingCycleResponse createCycleResponse(Instant periodEnd, boolean isDemo, int amount, int peakMembers) {
        return createCycleResponse(periodEnd, isDemo, amount, peakMembers, UUID.randomUUID());
    }

    private TenantInternalResponse createTenantInfo() {
        return new TenantInternalResponse(UUID.randomUUID(), "Nexus Ltd", "30-12345678-9", true, "nexus-ltd");
    }

    private SubscriptionPlanResponse createPlanResponse(int price) {
        return new SubscriptionPlanResponse(UUID.randomUUID(), "PREMIUM", price, null);
    }

    @Nested
    class FindAll {
        @Test
        void findAll_ValidPageable_ReturnsPagedResponses() {
            Pageable pageable = Pageable.unpaged();
            BillingCycleResponse response = createCycleResponse(Instant.now(), false, 50000, 100);
            Page<BillingCycleResponse> page = new PageImpl<>(List.of(response));

            when(billingCycleRepository.findBy(pageable, BillingCycleResponse.class)).thenReturn(page);

            Page<BillingCycleResponse> result = billingCycleService.findAll(pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(billingCycleRepository).findBy(pageable, BillingCycleResponse.class);
        }
    }

    @Nested
    class FindById {
        @Test
        void findById_CycleExists_ReturnsResponse() {
            UUID cycleId = UUID.randomUUID();
            BillingCycleResponse response = createCycleResponse(Instant.now(), false, 50000, 100);

            when(billingCycleRepository.findById(cycleId, BillingCycleResponse.class)).thenReturn(Optional.of(response));

            BillingCycleResponse result = billingCycleService.findById(cycleId);

            assertThat(result).isEqualTo(response);
        }

        @Test
        void findById_CycleNotExists_ThrowsBillingCycleNotFoundException() {
            UUID cycleId = UUID.randomUUID();

            when(billingCycleRepository.findById(cycleId, BillingCycleResponse.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingCycleService.findById(cycleId))
                    .isInstanceOf(BillingCycleNotFoundException.class);
        }
    }

    @Nested
    class HasValidCycle {
        @Test
        void hasValidCycle_IsDemoAndWithinPeriod_ReturnsTrue() {
            UUID tenantId = UUID.randomUUID();
            Instant futureEnd = Instant.now().plus(1, ChronoUnit.DAYS);
            BillingCycleResponse response = createCycleResponse(futureEnd, true, 0, 10);

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.of(response));

            boolean result = billingCycleService.hasValidCycle();

            assertThat(result).isTrue();
        }

        @Test
        void hasValidCycle_IsDemoAndPastPeriod_ReturnsFalse() {
            UUID tenantId = UUID.randomUUID();
            Instant pastEnd = Instant.now().minus(1, ChronoUnit.DAYS);
            BillingCycleResponse response = createCycleResponse(pastEnd, true, 0, 10);

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.of(response));

            boolean result = billingCycleService.hasValidCycle();

            assertThat(result).isFalse();
        }

        @Test
        void hasValidCycle_NotDemoAndWithinGracePeriod_ReturnsTrue() {
            UUID tenantId = UUID.randomUUID();
            Instant pastEndWithinGrace = Instant.now().plus(1, ChronoUnit.DAYS);
            BillingCycleResponse response = createCycleResponse(pastEndWithinGrace, false, 50000, 100);

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.of(response));

            boolean result = billingCycleService.hasValidCycle();

            assertThat(result).isTrue();
        }

        @Test
        void hasValidCycle_NotDemoAndPastGracePeriod_ReturnsFalse() {
            UUID tenantId = UUID.randomUUID();
            Instant pastEndBeyondGrace = Instant.now().minus(8, ChronoUnit.DAYS);
            BillingCycleResponse response = createCycleResponse(pastEndBeyondGrace, false, 50000, 100);

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.of(response));

            boolean result = billingCycleService.hasValidCycle();

            assertThat(result).isFalse();
        }

        @Test
        void hasValidCycle_PendingWithinGracePeriod_ReturnsTrue() {
            UUID tenantId = UUID.randomUUID();
            Instant periodStart = Instant.now().minus(5, ChronoUnit.DAYS);
            BillingCycleResponse response = new BillingCycleResponse(
                    UUID.randomUUID(), tenantId, UUID.randomUUID(), null,
                    periodStart, periodStart.plus(30, ChronoUnit.DAYS),
                    0, 0, 65000, CycleStatus.PENDING_PAYMENT, false);

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.of(response));

            boolean result = billingCycleService.hasValidCycle();

            assertThat(result).isTrue();
        }

        @Test
        void hasValidCycle_PendingPastGracePeriod_ReturnsFalse() {
            UUID tenantId = UUID.randomUUID();
            Instant periodStart = Instant.now().minus(10, ChronoUnit.DAYS);
            BillingCycleResponse response = new BillingCycleResponse(
                    UUID.randomUUID(), tenantId, UUID.randomUUID(), null,
                    periodStart, periodStart.plus(30, ChronoUnit.DAYS),
                    0, 0, 65000, CycleStatus.PENDING_PAYMENT, false);

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.of(response));

            boolean result = billingCycleService.hasValidCycle();

            assertThat(result).isFalse();
        }

        @Test
        void hasValidCycle_NoActiveCycle_ThrowsBillingCycleNotFoundException() {
            UUID tenantId = UUID.randomUUID();

            when(billingCycleRepository.findActive(eq(BillingCycleResponse.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingCycleService.hasValidCycle())
                    .isInstanceOf(BillingCycleNotFoundException.class);
        }
    }

    @Nested
    class ComputeNextCycleToPay {

        @Test
        void computeNextCycleToPay_ActivePaid_ReturnsCurrentCycle() {
            Instant periodEnd = Instant.now().plus(10, ChronoUnit.DAYS);
            TenantInternalResponse tenantInfo = createTenantInfo();
            UUID planId = UUID.randomUUID();
            BillingCycleResponse activeCycle = createCycleResponse(periodEnd, false, 99999, 100, planId);

            when(billingCycleRepository.findPayableCycles(eq(BillingCycleResponse.class)))
                    .thenReturn(List.of(activeCycle));
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(createPlanResponse(99999)));

            BillingCycleHydratedResponse result = billingCycleService.computeNextCycleToPay(tenantInfo);

            assertThat(result.status()).isEqualTo(CycleStatus.PAID);
            assertThat(result.isDemo()).isFalse();
            assertThat(result.periodEnd()).isEqualTo(periodEnd);
            assertThat(result.planId()).isEqualTo(planId);
            assertThat(result.price()).isEqualTo(99999);
            assertThat(result.peakMembers()).isEqualTo(100);
        }

        @Test
        void computeNextCycleToPay_PaidDemoCycle_ReturnsDemoCycle() {
            Instant periodEnd = Instant.now().plus(5, ChronoUnit.DAYS);
            TenantInternalResponse tenantInfo = createTenantInfo();
            UUID planId = UUID.randomUUID();
            BillingCycleResponse demoCycle = createCycleResponse(periodEnd, true, 0, 10, planId);
            SubscriptionPlanResponse plan = createPlanResponse(20000);

            when(billingCycleRepository.findPayableCycles(eq(BillingCycleResponse.class))).thenReturn(List.of(demoCycle));
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleHydratedResponse result = billingCycleService.computeNextCycleToPay(tenantInfo);

            assertThat(result.isDemo()).isTrue();
            assertThat(result.status()).isEqualTo(CycleStatus.PAID);
            assertThat(result.periodEnd()).isEqualTo(periodEnd);
            assertThat(result.planId()).isEqualTo(planId);
            assertThat(result.planTier()).isEqualTo("PREMIUM");
            assertThat(result.price()).isZero();
        }

        @Test
        void computeNextCycleToPay_Pending_ReturnsStoredValues() {
            UUID planId = UUID.randomUUID();
            Instant periodStart = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant periodEnd = Instant.now().plus(23, ChronoUnit.DAYS);
            BillingCycleResponse activeCycle = new BillingCycleResponse(
                    UUID.randomUUID(), UUID.randomUUID(), planId, null,
                    periodStart, periodEnd, 100, 0, 50000,
                    CycleStatus.PENDING_PAYMENT, false);
            TenantInternalResponse tenantInfo = createTenantInfo();
            SubscriptionPlanResponse plan = createPlanResponse(10000);

            when(billingCycleRepository.findPayableCycles(eq(BillingCycleResponse.class))).thenReturn(List.of(activeCycle));
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleHydratedResponse result = billingCycleService.computeNextCycleToPay(tenantInfo);

            assertThat(result.price()).isEqualTo(50000);
            assertThat(result.periodStart()).isEqualTo(periodStart);
            assertThat(result.periodEnd()).isEqualTo(periodEnd);
            assertThat(result.peakMembers()).isEqualTo(100);
            assertThat(result.discountPercentage()).isZero();
            assertThat(result.status()).isEqualTo(CycleStatus.PENDING_PAYMENT);
        }

        @Test
        void computeNextCycleToPay_PendingWithDiscount_ReturnsStoredValuesWithDiscount() {
            UUID planId = UUID.randomUUID();
            BillingCycleResponse activeCycle = new BillingCycleResponse(
                    UUID.randomUUID(), UUID.randomUUID(), planId, null,
                    Instant.now().minus(7, ChronoUnit.DAYS), Instant.now().plus(23, ChronoUnit.DAYS),
                    300, 15, 19200, CycleStatus.PENDING_PAYMENT, false);
            TenantInternalResponse tenantInfo = createTenantInfo();
            SubscriptionPlanResponse plan = createPlanResponse(10000);

            when(billingCycleRepository.findPayableCycles(eq(BillingCycleResponse.class))).thenReturn(List.of(activeCycle));
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleHydratedResponse result = billingCycleService.computeNextCycleToPay(tenantInfo);

            assertThat(result.price()).isEqualTo(19200);
            assertThat(result.discountPercentage()).isEqualTo(15);
            assertThat(result.peakMembers()).isEqualTo(300);
            assertThat(result.status()).isEqualTo(CycleStatus.PENDING_PAYMENT);
        }

        @Test
        void computeNextCycleToPay_Cancelled_SimulatesNewCycle() {
            UUID planId = UUID.randomUUID();
            BillingCycleResponse activeCycle = new BillingCycleResponse(
                    UUID.randomUUID(), UUID.randomUUID(), planId, null,
                    Instant.now().minus(30, ChronoUnit.DAYS), Instant.now(),
                    120, 10, 18000, CycleStatus.CANCELLED, false);
            TenantInternalResponse tenantInfo = createTenantInfo();
            SubscriptionPlanResponse plan = createPlanResponse(18000);

            when(billingCycleRepository.findPayableCycles(eq(BillingCycleResponse.class))).thenReturn(List.of(activeCycle));
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleHydratedResponse result = billingCycleService.computeNextCycleToPay(tenantInfo);

            assertThat(result.status()).isEqualTo(CycleStatus.PENDING_PAYMENT);
            assertThat(result.isDemo()).isFalse();
            assertThat(result.periodStart()).isBetween(
                    Instant.now().minus(1, ChronoUnit.MINUTES),
                    Instant.now().plus(1, ChronoUnit.MINUTES));
            assertThat(result.periodEnd()).isEqualTo(result.periodStart().plus(30, ChronoUnit.DAYS));
            assertThat(result.peakMembers()).isZero();
            assertThat(result.discountPercentage()).isZero();
            assertThat(result.price()).isEqualTo(18000);
        }

        @Test
        void computeNextCycleToPay_NoCycle_ThrowsBillingCycleNotFoundException() {
            TenantInternalResponse tenantInfo = createTenantInfo();

            when(billingCycleRepository.findPayableCycles(eq(BillingCycleResponse.class))).thenReturn(List.of());

            assertThatThrownBy(() -> billingCycleService.computeNextCycleToPay(tenantInfo))
                    .isInstanceOf(BillingCycleNotFoundException.class);
        }
    }

    @Nested
    class PayCycle {

        @Test
        void payCycle_PendingCycle_MarksPaid() {
            UUID tenantId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            BillingCycleEntity cycle = createCycle(cycleId, tenantId, CycleStatus.PENDING_PAYMENT, 50000);

            when(billingCycleRepository.findByIdForWrite(cycleId)).thenReturn(Optional.of(cycle));

            BillingCycleResponse response = billingCycleService.payCycle(new BillingCyclePayRequest(tenantId, cycleId, null));

            assertThat(cycle.getStatus()).isEqualTo(CycleStatus.PAID);
            assertThat(cycle.getAmount()).isEqualTo(50000);
            assertThat(response.status()).isEqualTo(CycleStatus.PAID);
            assertThat(response.amount()).isEqualTo(50000);
            verify(billingCycleRepository, never()).save(any());
        }

        @Test
        void payCycle_PendingCycleWithExtra_AddsExtraToAmount() {
            UUID tenantId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            BillingCycleEntity cycle = createCycle(cycleId, tenantId, CycleStatus.PENDING_PAYMENT, 50000);

            when(billingCycleRepository.findByIdForWrite(cycleId)).thenReturn(Optional.of(cycle));

            BillingCycleResponse response = billingCycleService.payCycle(new BillingCyclePayRequest(tenantId, cycleId, 7000));

            assertThat(cycle.getStatus()).isEqualTo(CycleStatus.PAID);
            assertThat(cycle.getAmount()).isEqualTo(57000);
            assertThat(response.status()).isEqualTo(CycleStatus.PAID);
            assertThat(response.amount()).isEqualTo(57000);
            verify(billingCycleRepository, never()).save(any());
        }

        @Test
        void payCycle_AlreadyPaid_ThrowsCycleAlreadyPaidException() {
            UUID tenantId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            BillingCycleEntity cycle = createCycle(cycleId, tenantId, CycleStatus.PAID, 50000);

            when(billingCycleRepository.findByIdForWrite(cycleId)).thenReturn(Optional.of(cycle));

            assertThatThrownBy(() -> billingCycleService.payCycle(new BillingCyclePayRequest(tenantId, cycleId, null)))
                    .isInstanceOf(CycleAlreadyPaidException.class);
        }

        @Test
        void payCycle_CancelledCycle_ThrowsBillingCycleNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            BillingCycleEntity cycle = createCycle(cycleId, tenantId, CycleStatus.CANCELLED, 0);

            when(billingCycleRepository.findByIdForWrite(cycleId)).thenReturn(Optional.of(cycle));

            assertThatThrownBy(() -> billingCycleService.payCycle(new BillingCyclePayRequest(tenantId, cycleId, null)))
                    .isInstanceOf(BillingCycleNotFoundException.class);
        }

        @Test
        void payCycle_CycleNotFound_ThrowsBillingCycleNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();

            when(billingCycleRepository.findByIdForWrite(cycleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingCycleService.payCycle(new BillingCyclePayRequest(tenantId, cycleId, null)))
                    .isInstanceOf(BillingCycleNotFoundException.class);
        }
    }

    @Nested
    class PayFirstCycle {

        @Test
        void payFirstCycle_NoActiveCycle_CreatesNewPaidCycle() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            SubscriptionPlanResponse plan = new SubscriptionPlanResponse(planId, "PREMIUM", 50000, null);

            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.empty());
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleResponse response = billingCycleService.payFirstCycle(new BillingCyclePayFirstNonDemoRequest(tenantId, planId, null, null, null));

            ArgumentCaptor<BillingCycleEntity> captor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(captor.capture());

            BillingCycleEntity savedCycle = captor.getValue();
            assertThat(savedCycle.getStatus()).isEqualTo(CycleStatus.PAID);
            assertThat(savedCycle.getTenantId()).isEqualTo(tenantId);
            assertThat(savedCycle.getPlanId()).isEqualTo(planId);
            assertThat(savedCycle.getAmount()).isEqualTo(50000);
            assertThat(savedCycle.getDiscountPercentage()).isZero();
            assertThat(savedCycle.getRemainingDiscountMonths()).isZero();
            assertThat(savedCycle.isDemo()).isFalse();
            assertThat(savedCycle.getPeriodEnd()).isNotNull();
            assertThat(response.id()).isEqualTo(savedCycle.getId());
            assertThat(response.status()).isEqualTo(CycleStatus.PAID);
            assertThat(response.amount()).isEqualTo(50000);
        }

        @Test
        void payFirstCycle_ActiveDemoCycle_TerminatesDemoAndCreatesNewCycle() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            BillingCycleEntity demoCycle = new BillingCycleEntity();
            demoCycle.setDemo(true);
            demoCycle.setStatus(CycleStatus.PAID);
            demoCycle.setTenantId(tenantId);
            SubscriptionPlanResponse plan = new SubscriptionPlanResponse(planId, "PREMIUM", 50000, null);

            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(demoCycle));
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleResponse response = billingCycleService.payFirstCycle(new BillingCyclePayFirstNonDemoRequest(tenantId, planId, null, null, null));

            assertThat(demoCycle.getStatus()).isEqualTo(CycleStatus.COMPLETED);
            assertThat(demoCycle.getPeriodEnd()).isNotNull();

            ArgumentCaptor<BillingCycleEntity> captor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository, times(2)).save(captor.capture());

            BillingCycleEntity newCycle = captor.getAllValues().get(1);
            assertThat(newCycle.getStatus()).isEqualTo(CycleStatus.PAID);
            assertThat(newCycle.getTenantId()).isEqualTo(tenantId);
            assertThat(newCycle.getPlanId()).isEqualTo(planId);
            assertThat(newCycle.isDemo()).isFalse();
            assertThat(response.id()).isEqualTo(newCycle.getId());
            assertThat(response.status()).isEqualTo(CycleStatus.PAID);
        }

        @Test
        void payFirstCycle_WithDiscount_CreatesNewCycleWithDiscountedPrice() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            SubscriptionPlanResponse plan = new SubscriptionPlanResponse(planId, "PREMIUM", 50000, null);

            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.empty());
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleResponse response = billingCycleService.payFirstCycle(new BillingCyclePayFirstNonDemoRequest(tenantId, planId, null, 15, null));

            ArgumentCaptor<BillingCycleEntity> captor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(captor.capture());

            BillingCycleEntity savedCycle = captor.getValue();
            assertThat(savedCycle.getDiscountPercentage()).isEqualTo(15);
            assertThat(savedCycle.getAmount()).isEqualTo(42500);
            assertThat(response.discountPercentage()).isEqualTo(15);
            assertThat(response.amount()).isEqualTo(42500);
        }

        @Test
        void payFirstCycle_WithCustomAmount_UsesCustomAmount() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            SubscriptionPlanResponse plan = new SubscriptionPlanResponse(planId, "PREMIUM", 50000, null);

            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.empty());
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleResponse response = billingCycleService.payFirstCycle(new BillingCyclePayFirstNonDemoRequest(tenantId, planId, 25000, null, null));

            ArgumentCaptor<BillingCycleEntity> captor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(captor.capture());

            BillingCycleEntity savedCycle = captor.getValue();
            assertThat(savedCycle.getAmount()).isEqualTo(25000);
            assertThat(response.amount()).isEqualTo(25000);
        }

        @Test
        void payFirstCycle_WithRemainingDiscountMonths_SetsRemainingMonths() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            SubscriptionPlanResponse plan = new SubscriptionPlanResponse(planId, "PREMIUM", 50000, null);

            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.empty());
            when(subscriptionPlanRepository.findById(eq(planId), eq(SubscriptionPlanResponse.class))).thenReturn(Optional.of(plan));

            BillingCycleResponse response = billingCycleService.payFirstCycle(new BillingCyclePayFirstNonDemoRequest(tenantId, planId, null, null, 6));

            ArgumentCaptor<BillingCycleEntity> captor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(captor.capture());

            BillingCycleEntity savedCycle = captor.getValue();
            assertThat(savedCycle.getRemainingDiscountMonths()).isEqualTo(6);
            assertThat(response.discountPercentage()).isZero();
        }
    }

    @Nested
    class ChangePlan {

        private SubscriptionPlanEntity createPlanEntity(UUID planId, String tier, Integer maxMembers, int price, boolean active) {
            SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
            plan.setId(planId);
            plan.setTier(tier);
            plan.setMaxMembers(maxMembers);
            plan.setPrice(price);
            plan.setIsActive(active);
            return plan;
        }

        @Test
        void changePlan_ValidDowngrade_SchedulesNextPlanOnActiveCycle() {
            UUID tenantId = UUID.randomUUID();
            UUID currentPlanId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "BASE", 250, 20000, true);
            BillingCycleEntity activeCycle = createCycle(UUID.randomUUID(), tenantId, CycleStatus.PENDING_PAYMENT, 50000);
            activeCycle.setPlanId(currentPlanId);
            activeCycle.setDiscountPercentage(10);
            activeCycle.setPeakMembers(150);
            activeCycle.setRemainingDiscountMonths(3);
            Instant periodStart = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant periodEnd = Instant.now().plus(23, ChronoUnit.DAYS);
            activeCycle.setPeriodStart(periodStart);
            activeCycle.setPeriodEnd(periodEnd);

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(100);
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(activeCycle));

            BillingCycleResponse response = billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId));

            assertThat(activeCycle.getStatus()).isEqualTo(CycleStatus.PENDING_PAYMENT);
            assertThat(activeCycle.getPlanId()).isEqualTo(currentPlanId);
            assertThat(activeCycle.getNextPlanId()).isEqualTo(targetPlanId);
            assertThat(activeCycle.getAmount()).isEqualTo(50000);
            assertThat(activeCycle.getDiscountPercentage()).isEqualTo(10);
            assertThat(activeCycle.getRemainingDiscountMonths()).isEqualTo(3);
            assertThat(activeCycle.getPeakMembers()).isEqualTo(150);
            assertThat(activeCycle.getPeriodStart()).isEqualTo(periodStart);
            assertThat(activeCycle.getPeriodEnd()).isEqualTo(periodEnd);

            verify(billingCycleRepository, times(1)).save(activeCycle);
            assertThat(response.id()).isEqualTo(activeCycle.getId());
            assertThat(response.planId()).isEqualTo(currentPlanId);
            assertThat(response.nextPlanId()).isEqualTo(targetPlanId);
            assertThat(response.amount()).isEqualTo(50000);
            assertThat(response.status()).isEqualTo(CycleStatus.PENDING_PAYMENT);
        }

        @Test
        void changePlan_MemberCountExceedsTargetLimit_ThrowsPlanMemberLimitExceededException() {
            UUID tenantId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "BASE", 250, 20000, true);

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(300);

            assertThatThrownBy(() -> billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId)))
                    .isInstanceOf(PlanMemberLimitExceededException.class);

            verify(billingCycleRepository, never()).findActiveForWrite(eq(BillingCycleEntity.class));
        }

        @Test
        void changePlan_UnlimitedTargetPlan_AllowsAnyMemberCount() {
            UUID tenantId = UUID.randomUUID();
            UUID currentPlanId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "ILIMITADO", null, 80000, true);
            BillingCycleEntity activeCycle = createCycle(UUID.randomUUID(), tenantId, CycleStatus.PENDING_PAYMENT, 50000);
            activeCycle.setPlanId(currentPlanId);

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(3000);
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(activeCycle));

            BillingCycleResponse response = billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId));

            assertThat(activeCycle.getStatus()).isEqualTo(CycleStatus.PENDING_PAYMENT);
            assertThat(activeCycle.getPlanId()).isEqualTo(currentPlanId);
            assertThat(activeCycle.getNextPlanId()).isEqualTo(targetPlanId);
            verify(billingCycleRepository, times(1)).save(activeCycle);
            assertThat(response.id()).isEqualTo(activeCycle.getId());
            assertThat(response.planId()).isEqualTo(currentPlanId);
            assertThat(response.nextPlanId()).isEqualTo(targetPlanId);
        }

        @Test
        void changePlan_LegacyPlan_ThrowsSubscriptionPlanNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "LEGADO", null, 30000, true);

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));

            assertThatThrownBy(() -> billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId)))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);

            verify(billingCycleRepository, never()).findActiveForWrite(eq(BillingCycleEntity.class));
        }

        @Test
        void changePlan_InactiveTargetPlan_ThrowsSubscriptionPlanNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "BASE", 250, 20000, false);

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));

            assertThatThrownBy(() -> billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId)))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);

            verify(billingCycleRepository, never()).findActiveForWrite(eq(BillingCycleEntity.class));
        }

        @Test
        void changePlan_PaidActiveCycle_SchedulesNextPlan() {
            UUID tenantId = UUID.randomUUID();
            UUID currentPlanId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "BASE", 250, 20000, true);
            BillingCycleEntity activeCycle = createCycle(UUID.randomUUID(), tenantId, CycleStatus.PAID, 50000);
            activeCycle.setPlanId(currentPlanId);

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(100);
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(activeCycle));

            BillingCycleResponse response = billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId));

            assertThat(activeCycle.getStatus()).isEqualTo(CycleStatus.PAID);
            assertThat(activeCycle.getPlanId()).isEqualTo(currentPlanId);
            assertThat(activeCycle.getNextPlanId()).isEqualTo(targetPlanId);
            assertThat(activeCycle.getAmount()).isEqualTo(50000);
            verify(billingCycleRepository, times(1)).save(activeCycle);
            assertThat(response.nextPlanId()).isEqualTo(targetPlanId);
            assertThat(response.status()).isEqualTo(CycleStatus.PAID);
        }

        @Test
        void changePlan_NextPlanAlreadyScheduled_ThrowsPlanChangeAlreadyScheduledException() {
            UUID tenantId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();
            SubscriptionPlanEntity targetPlan = createPlanEntity(targetPlanId, "BASE", 250, 20000, true);
            BillingCycleEntity activeCycle = createCycle(UUID.randomUUID(), tenantId, CycleStatus.PAID, 50000);
            activeCycle.setNextPlanId(UUID.randomUUID());

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.of(targetPlan));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(100);
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(activeCycle));

            assertThatThrownBy(() -> billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId)))
                    .isInstanceOf(PlanChangeAlreadyScheduledException.class);

            verify(billingCycleRepository, never()).save(any());
        }

        @Test
        void changePlan_PlanNotFound_ThrowsSubscriptionPlanNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID targetPlanId = UUID.randomUUID();

            when(subscriptionPlanRepository.findById(targetPlanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingCycleService.changePlan(tenantId, new TenantChangePlanRequest(targetPlanId)))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);
        }
    }

    private BillingCycleEntity createCycle(UUID cycleId, UUID tenantId, CycleStatus status, int amount) {
        BillingCycleEntity cycle = new BillingCycleEntity();
        cycle.setId(cycleId);
        cycle.setTenantId(tenantId);
        cycle.setStatus(status);
        cycle.setAmount(amount);
        return cycle;
    }
}
