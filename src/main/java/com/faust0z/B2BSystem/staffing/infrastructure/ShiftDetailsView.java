package com.faust0z.B2BSystem.staffing.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import com.faust0z.B2BSystem.staffing.web.ShiftResponse;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_shift_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftDetailsView {
    @Id
    private UUID id;
    @TenantId
    private UUID tenantId;
    private UUID staffId;
    private String firstName;
    private String lastName;
    private Instant clockInAt;
    private Instant clockOutAt;
    private boolean autoClosed;
    private boolean isActive;
    private UUID locationId;

    public ShiftResponse toResponse() {
        return new ShiftResponse(id, staffId, firstName + " " + lastName, clockInAt, clockOutAt, autoClosed, isActive, locationId);
    }
}
