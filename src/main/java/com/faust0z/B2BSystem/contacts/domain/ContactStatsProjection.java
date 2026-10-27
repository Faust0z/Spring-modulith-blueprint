package com.faust0z.B2BSystem.contacts.domain;

public record ContactStatsProjection(
        Long total,
        Long active,
        Long expired
) {
}
