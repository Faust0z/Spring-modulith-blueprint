package com.faust0z.B2BSystem.contacts.domain;

public record ContactQuickStatsProjection(
        Long total,
        Long expired,
        Long withDebt,
        Long notCheckedIn
) {
}
