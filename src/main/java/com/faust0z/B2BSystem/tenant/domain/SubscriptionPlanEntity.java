package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.TenantTogglableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity extends TenantTogglableEntity {
    @Column(name = "tier")
    private String tier;

    @Column(name = "price")
    private int price = 0;

    @Column(name = "max_members")
    private Integer maxMembers;
}