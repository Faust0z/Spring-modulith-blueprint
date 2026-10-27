package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.domain.BillingCycleEntity;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.tenant.application.BillingDemoCycleService;
@ExtendWith(MockitoExtension.class)
class BillingDemoCycleServiceTest {

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private BillingDemoCycleService billingDemoCycleService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private SubscriptionPlanEntity createActiveGymPlan(UUID planId) {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setId(planId);
        plan.setIsActive(true);
        return plan;
    }

    @Nested
    class CreateDemoCycle {

        @Test
        void createDemoCycle_ValidPlan_SavesDemoCycleWithChosenPlan() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();

            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(createActiveGymPlan(planId)));

            billingDemoCycleService.createDemoCycle(tenantId, planId);

            ArgumentCaptor<BillingCycleEntity> cycleCaptor = ArgumentCaptor.forClass(BillingCycleEntity.class);
            verify(billingCycleRepository).save(cycleCaptor.capture());

            BillingCycleEntity savedCycle = cycleCaptor.getValue();
            assertThat(savedCycle.getTenantId()).isEqualTo(tenantId);
            assertThat(savedCycle.getPlanId()).isEqualTo(planId);
            assertThat(savedCycle.isDemo()).isTrue();
            assertThat(savedCycle.getStatus()).isEqualTo(CycleStatus.PAID);

            LocalDate today = Instant.now().atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDate();
            Instant expectedStart = today.atStartOfDay(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();
            Instant expectedEnd = today.plusDays(7).atTime(23, 59, 59).atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();

            assertThat(savedCycle.getPeriodStart()).isEqualTo(expectedStart);
            assertThat(savedCycle.getPeriodEnd()).isEqualTo(expectedEnd);
            verify(subscriptionPlanRepository, never()).save(any(SubscriptionPlanEntity.class));
        }

        @Test
        void createDemoCycle_PlanNotFound_ThrowsSubscriptionPlanNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();

            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingDemoCycleService.createDemoCycle(tenantId, planId))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);

            verify(billingCycleRepository, never()).save(any());
        }

        @Test
        void createDemoCycle_InactivePlan_ThrowsSubscriptionPlanNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            SubscriptionPlanEntity plan = createActiveGymPlan(planId);
            plan.setIsActive(false);

            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> billingDemoCycleService.createDemoCycle(tenantId, planId))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);

            verify(billingCycleRepository, never()).save(any());
        }

        @Test
        void createDemoCycle_NonGymPlan_SavesDemoCycleWithChosenPlan() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            SubscriptionPlanEntity plan = createActiveGymPlan(planId);

            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

            billingDemoCycleService.createDemoCycle(tenantId, planId);

            verify(billingCycleRepository).save(any(BillingCycleEntity.class));
        }
    }
}
