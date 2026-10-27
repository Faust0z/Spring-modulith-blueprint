package com.faust0z.B2BSystem.contacts.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quick numeric stats for contacts dashboard")
public record ContactQuickStatsResponse(
        @Schema(description = "Total contacts") long totalContacts,
        @Schema(description = "Contacts expiring in next 7 days (excludes already expired)") long expiredContacts,
        @Schema(description = "Contacts with debt > 0") long contactsWithDebt,
        @Schema(description = "Contacts not checked in last 7 days or never") long notCheckedIn
) {
}
