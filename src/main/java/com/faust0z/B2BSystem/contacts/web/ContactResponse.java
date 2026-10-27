package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.contacts.domain.GenderCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Contact response")
public record ContactResponse(
        @Schema(description = "Contact ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "DNI", example = "22097162")
        String dni,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Turner")
        String lastName,

        @Schema(description = "Full name", example = "Juan Pérez")
        String fullName,

        @Schema(description = "Current membership name", example = "Plan Oro")
        String membershipName,

        @Schema(description = "Access expiration date", example = "2026-12-31T23:59:59Z")
        Instant accessUntil,

        @Schema(description = "Can access", example = "false")
        Boolean canAccess,

        @Schema(description = "Access token", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID accessToken,

        @Schema(description = "Is active", example = "true")
        Boolean isActive,

        @Schema(description = "Created at", example = "2026-01-01T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Email", example = "john@example.com")
        String email,

        @Schema(description = "Birth date", example = "1995-05-20")
        LocalDate birthDate,

        @Schema(description = "Phone number", example = "+543329771432")
        String phoneNumber,

        @Schema(description = "Gender", example = "MASCULINO")
        GenderCategory gender,

        @Schema(description = "Last scanned at", example = "2026-12-31T23:59:59Z")
        Instant lastScannedAt,

        @Schema(description = "Debt amount", example = "3000")
        Integer debtAmount
) {
}
