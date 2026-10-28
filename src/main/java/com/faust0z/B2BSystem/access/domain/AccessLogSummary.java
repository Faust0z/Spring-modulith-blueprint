package com.faust0z.B2BSystem.access.domain;

import java.time.Instant;
import java.util.UUID;

public record AccessLogSummary(
        UUID id,
        UUID tenantId,
        UUID contactId,
        String dni,
        String firstName,
        String lastName,
        String currentMembershipName,
        Instant accessUntil,
        Instant scannedAt,
        AccessStatus status,
        Integer memberDebtAmount,
        UUID locationId
) {
}