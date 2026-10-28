package com.faust0z.B2BSystem.contacts.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContactSummary(
        UUID id,
        UUID tenantId,
        String dni,
        String firstName,
        String lastName,
        Instant accessUntil,
        String currentMembershipName,
        UUID accessToken,
        Instant createdAt,
        String email,
        LocalDate birthDate,
        String phoneNumber,
        GenderCategory gender,
        int debtAmount
) {
}