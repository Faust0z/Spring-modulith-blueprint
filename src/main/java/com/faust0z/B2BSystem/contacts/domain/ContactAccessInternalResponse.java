package com.faust0z.B2BSystem.contacts.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Contact internal response")
public record ContactAccessInternalResponse(
        @Schema(description = "Contact ID", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Turner")
        String lastName,

        @Schema(description = "Current membership name")
        String currentMembershipName,

        @Schema(description = "Access expiration date", example = "2026-12-31T23:59:59Z")
        Instant accessUntil,

        @Schema(description = "Contact debt", example = "3000")
        Integer debtAmount,

        boolean isActive
) {
}
