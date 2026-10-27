package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.contacts.domain.GenderCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Contact filter request")
public record ContactFilterRequest(
        @Schema(description = "Search by DNI, name, or membership name", example = "22097162")
        String searchTerm,

        @Schema(description = "Filter by active status", example = "true")
        Boolean isActive,

        @Schema(description = "Filter by gender", example = "MASCULINO")
        GenderCategory gender,

        @Schema(description = "Access expiration start", example = "2026-12-31T23:59:59Z")
        Instant hasAccessBegin,

        @Schema(description = "Access expiration end", example = "2026-12-31T23:59:59Z")
        Instant hasAccessEnd,

        @Schema(description = "Last scanned before date", example = "2026-12-31T23:59:59Z")
        Instant lastScannedBefore,

        @Schema(description = "Filter by debt > 0", example = "true")
        Boolean hasDebt,

        @Schema(description = "Filter by contacts that have entered at least once", example = "true")
        Boolean hasEntered
) {
}
