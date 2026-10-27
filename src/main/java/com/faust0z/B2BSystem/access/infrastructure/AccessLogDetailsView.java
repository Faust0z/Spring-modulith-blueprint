package com.faust0z.B2BSystem.access.infrastructure;

import com.faust0z.B2BSystem.access.domain.AccessStatus;
import com.faust0z.B2BSystem.access.web.AccessLogResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_access_log_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessLogDetailsView {
    @Id
    private UUID id;
    @TenantId
    private UUID tenantId;
    private UUID contactId;
    private String dni;
    private String firstName;
    private String lastName;
    private String current_membership_name;
    private Instant access_until;
    private Instant scannedAt;
    @Enumerated(EnumType.STRING)
    private AccessStatus status;
    private Integer memberDebtAmount;
    private UUID locationId;

    public AccessLogResponse toResponse() {
        return new AccessLogResponse(id, contactId, firstName + " " + lastName, access_until, scannedAt, status, memberDebtAmount,
                locationId);
    }
}
