package com.faust0z.B2BSystem.staffing.domain;

import com.faust0z.B2BSystem.core.ImmutableBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "shifts")
public class ShiftEntity extends ImmutableBaseEntity {
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false, updatable = false)
    private UUID staffId;

    @CreationTimestamp
    @Column(name = "clock_in_at", nullable = false, updatable = false)
    private Instant clockInAt;

    @Column(name = "clock_out_at")
    private Instant clockOutAt;

    @Column(name = "auto_closed", nullable = false)
    private boolean autoClosed = false;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;
}
