package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.TenantMutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "billing_cycles")
public class BillingCycleEntity extends TenantMutableEntity {

    @Column(name = "plan_id", updatable = false)
    private UUID planId;

    @Column(name = "next_plan_id")
    private UUID nextPlanId;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "peak_members", nullable = false)
    private int peakMembers = 0;

    @Column(name = "remaining_discount_months", nullable = false)
    private int remainingDiscountMonths = 0;

    @Column(name = "discount_percentage", nullable = false)
    private int discountPercentage = 0;

    @Column(name = "amount", nullable = false)
    private int amount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CycleStatus status;

    @Column(name = "is_demo", nullable = false)
    private boolean isDemo = false;
}
