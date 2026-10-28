package com.faust0z.B2BSystem.catalog.domain;

import com.faust0z.B2BSystem.core.TenantSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_plans")
@SQLRestriction("deleted_at IS NULL")
public class ServicePlanEntity extends TenantSoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
