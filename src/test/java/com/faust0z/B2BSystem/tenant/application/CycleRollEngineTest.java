package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.infrastructure.BillingCycleRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.tenant.application.CycleRollEngine;
@ExtendWith(MockitoExtension.class)
class CycleRollEngineTest {

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private ContactInternalApi memberApi;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CycleRollEngine cycleRollEngine;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private BillingCycleEntity createActiveCycleEntity(UUID tenantId, int peakMembers, Instant periodEnd) {
        BillingCycleEntity cycle = new BillingCycleEntity();
        cycle.setId(UUID.randomUUID());
        cycle.setTenantId(tenantId);
        cycle.setPlanId(UUID.randomUUID());
        cycle.setPeakMembers(peakMembers);
        cycle.setPeriodEnd(periodEnd);
        cycle.setAmount(20000);
        cycle.setStatus(CycleStatus.PAID);
        return cycle;
    }

    private BillingCycleEntity createActiveCycleEntity(UUID tenantId, int peakMembers) {
        return createActiveCycleEntity(tenantId, peakMembers, Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private SubscriptionPlanEntity createPlanEntity(String tier, Integer maxMembers, int price) {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setId(UUID.randomUUID());
        plan.setTier(tier);
        plan.setMaxMembers(maxMembers);
        plan.setPrice(price);
        plan.setIsActive(true);
        return plan;
    }

    @SuppressWarnings("unchecked")
    private void mockTransactionTemplate() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any(Consumer.class));
    }

    @Nested
    class RecordNightlyPeaks {

        @Test
        void recordNightlyPeaks_HeadcountExceedsCurrentPeak_UpdatesPeakMembers() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity cycle = createActiveCycleEntity(tenantId, 100);

            mockTransactionTemplate();
            when(billingCycleRepository.findActivesNative(any(Instant.class))).thenReturn(List.of(cycle));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(150);

            cycleRollEngine.recordNightlyPeaks();

            assertThat(cycle.getPeakMembers()).isEqualTo(150);
            verify(billingCycleRepository).save(cycle);
        }

        @Test
        void recordNightlyPeaks_HeadcountLowerThanCurrentPeak_RetainsOriginalPeak() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity cycle = createActiveCycleEntity(tenantId, 100);

            mockTransactionTemplate();
            when(billingCycleRepository.findActivesNative(any(Instant.class))).thenReturn(List.of(cycle));
            when(memberApi.countContactsWithAccess(any(Instant.class))).thenReturn(50);

            cycleRollEngine.recordNightlyPeaks();

            assertThat(cycle.getPeakMembers()).isEqualTo(100);
            verify(billingCycleRepository, never()).save(any());
        }
    }

    @Nested
    class ExecuteNightlyRoll {

        @Test
        void executeNightlyRoll_ExpiredTenantsExist_TriggersRollSingleTenantForEach() {
            UUID tenantId1 = UUID.randomUUID();
            UUID tenantId2 = UUID.randomUUID();
            BillingCycleEntity cycle1 = createActiveCycleEntity(tenantId1, 50);
            BillingCycleEntity cycle2 = createActiveCycleEntity(tenantId2, 60);
            SubscriptionPlanEntity planResponse = createPlanEntity("BASE", 250, 10000);

            mockTransactionTemplate();
            when(billingCycleRepository.findTenantIdsWithExpiredActive(any(Instant.class)))
                    .thenReturn(List.of(tenantId1, tenantId2));
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class)))
                    .thenReturn(Optional.of(cycle1), Optional.of(cycle2));
            when(subscriptionPlanRepository.findById(any(UUID.class))).thenReturn(Optional.of(planResponse));

            cycleRollEngine.executeNightlyRoll();

            assertThat(cycle1.getStatus()).isEqualTo(CycleStatus.COMPLETED);
            assertThat(cycle2.getStatus()).isEqualTo(CycleStatus.COMPLETED);
            verify(billingCycleRepository, times(2)).save(any(BillingCycleEntity.class));
        }
    }

    @Nested
    class RollSingleTenant {

        @Test
        void rollSingleTenant_HasDiscount_CompletesOldAndCreatesActiveUnpaidWithDiscountedPrice() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 300);
            oldCycle.setRemainingDiscountMonths(2);
            oldCycle.setDiscountPercentage(15);
            SubscriptionPlanEntity planResponse = createPlanEntity("BASE", 250, 20000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(planResponse));

            cycleRollEngine.rollSingleTenant(tenantId);

            assertThat(oldCycle.getStatus()).isEqualTo(CycleStatus.COMPLETED);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getStatus()).isEqualTo(CycleStatus.PENDING_PAYMENT);
            assertThat(newCycle.getDiscountPercentage()).isEqualTo(15);
            assertThat(newCycle.getRemainingDiscountMonths()).isEqualTo(1);
            assertThat(newCycle.getPlanId()).isEqualTo(planResponse.getId());

            int expectedCost = (int) (20000 * 0.85);
            assertThat(newCycle.getAmount()).isEqualTo(expectedCost);

            Instant expectedEnd = oldCycle.getPeriodEnd().plus(30, ChronoUnit.DAYS)
                    .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                    .toLocalDate()
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                    .toInstant();
            assertThat(newCycle.getPeriodEnd()).isEqualTo(expectedEnd);
        }

        @Test
        void rollSingleTenant_NoDiscount_CreatesActiveUnpaidWithPlanPrice() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 100);
            SubscriptionPlanEntity planResponse = createPlanEntity("BASE", 250, 20000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(planResponse));

            cycleRollEngine.rollSingleTenant(tenantId);

            assertThat(oldCycle.getStatus()).isEqualTo(CycleStatus.COMPLETED);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getStatus()).isEqualTo(CycleStatus.PENDING_PAYMENT);
            assertThat(newCycle.getPlanId()).isEqualTo(planResponse.getId());
            assertThat(newCycle.getAmount()).isEqualTo(20000);
            assertThat(newCycle.getDiscountPercentage()).isZero();
            assertThat(newCycle.getRemainingDiscountMonths()).isZero();
        }

        @Test
        void rollSingleTenant_ExceedsPlanLimit_AutoUpgradesToNextTier() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 300);
            SubscriptionPlanEntity basePlan = createPlanEntity("BASE", 250, 20000);
            SubscriptionPlanEntity plusPlan = createPlanEntity("PLUS", 500, 35000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(basePlan));
            when(subscriptionPlanRepository.findByIsActiveTrue()).thenReturn(List.of(basePlan, plusPlan));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPlanId()).isEqualTo(plusPlan.getId());
            assertThat(newCycle.getAmount()).isEqualTo(35000);
        }

        @Test
        void rollSingleTenant_NoTierCoversPeak_AutoUpgradesToUnlimitedPlan() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 600);
            SubscriptionPlanEntity basePlan = createPlanEntity("BASE", 250, 20000);
            SubscriptionPlanEntity unlimitedPlan = createPlanEntity("ILIMITADO", null, 80000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(basePlan));
            when(subscriptionPlanRepository.findByIsActiveTrue()).thenReturn(List.of(basePlan, unlimitedPlan));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPlanId()).isEqualTo(unlimitedPlan.getId());
            assertThat(newCycle.getAmount()).isEqualTo(80000);
        }

        @Test
        void rollSingleTenant_WithinPlanLimit_KeepsCurrentPlan() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 100);
            SubscriptionPlanEntity basePlan = createPlanEntity("BASE", 250, 20000);
            SubscriptionPlanEntity plusPlan = createPlanEntity("PLUS", 500, 35000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(basePlan));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPlanId()).isEqualTo(basePlan.getId());
            assertThat(newCycle.getAmount()).isEqualTo(20000);
            verify(subscriptionPlanRepository, never()).findByIsActiveTrue();
        }

        @Test
        void rollSingleTenant_LegacyPlan_KeepsFixedPriceWithoutUpgrade() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 900);
            SubscriptionPlanEntity legacyPlan = createPlanEntity("LEGADO", null, 40000);
            SubscriptionPlanEntity plusPlan = createPlanEntity("PLUS", 500, 35000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(legacyPlan));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPlanId()).isEqualTo(legacyPlan.getId());
            assertThat(newCycle.getAmount()).isEqualTo(40000);
            verify(subscriptionPlanRepository, never()).findByIsActiveTrue();
        }

        @Test
        void rollSingleTenant_UnpaidCycle_CancelsWithoutCreatingNewCycle() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity unpaidCycle = createActiveCycleEntity(tenantId, 100);
            unpaidCycle.setStatus(CycleStatus.PENDING_PAYMENT);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(unpaidCycle));

            cycleRollEngine.rollSingleTenant(tenantId);

            assertThat(unpaidCycle.getStatus()).isEqualTo(CycleStatus.CANCELLED);
            verify(billingCycleRepository, never()).save(any(BillingCycleEntity.class));
        }

        @Test
        void rollSingleTenant_PunishmentCutoff_GapExceeds30Days_StartsFromNow() {
            UUID tenantId = UUID.randomUUID();
            Instant oldPeriodEnd = Instant.now().minus(31, ChronoUnit.DAYS);
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 100, oldPeriodEnd);
            SubscriptionPlanEntity planResponse = createPlanEntity("BASE", 250, 10000);

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(oldCycle.getPlanId())).thenReturn(Optional.of(planResponse));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPeriodStart()).isAfter(oldPeriodEnd);
            assertThat(newCycle.getPeriodStart()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        void rollSingleTenant_ScheduledNextPlanFitsPeak_SeedsNewCycleWithNextPlan() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 100);
            SubscriptionPlanEntity currentPlan = createPlanEntity("PLUS", 500, 35000);
            SubscriptionPlanEntity nextPlan = createPlanEntity("BASE", 250, 20000);
            oldCycle.setPlanId(currentPlan.getId());
            oldCycle.setNextPlanId(nextPlan.getId());

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(nextPlan.getId())).thenReturn(Optional.of(nextPlan));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPlanId()).isEqualTo(nextPlan.getId());
            assertThat(newCycle.getAmount()).isEqualTo(20000);
            assertThat(newCycle.getNextPlanId()).isNull();
            verify(subscriptionPlanRepository, never()).findById(currentPlan.getId());
            verify(subscriptionPlanRepository, never()).findByIsActiveTrue();
        }

        @Test
        void rollSingleTenant_ScheduledNextPlanExceedsPeak_AutoUpgradesBeyondNextPlan() {
            UUID tenantId = UUID.randomUUID();
            BillingCycleEntity oldCycle = createActiveCycleEntity(tenantId, 300);
            SubscriptionPlanEntity currentPlan = createPlanEntity("PLUS", 500, 35000);
            SubscriptionPlanEntity nextPlan = createPlanEntity("BASE", 250, 20000);
            SubscriptionPlanEntity plusPlan = createPlanEntity("PLUS", 500, 35000);
            oldCycle.setPlanId(currentPlan.getId());
            oldCycle.setNextPlanId(nextPlan.getId());

            mockTransactionTemplate();
            when(billingCycleRepository.findActiveForWrite(eq(BillingCycleEntity.class))).thenReturn(Optional.of(oldCycle));
            when(subscriptionPlanRepository.findById(nextPlan.getId())).thenReturn(Optional.of(nextPlan));
            when(subscriptionPlanRepository.findByIsActiveTrue()).thenReturn(List.of(nextPlan, plusPlan));

            cycleRollEngine.rollSingleTenant(tenantId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity newCycle = cycleCaptor.getValue();
            assertThat(newCycle.getPlanId()).isEqualTo(plusPlan.getId());
            assertThat(newCycle.getAmount()).isEqualTo(35000);
        }
    }
}
