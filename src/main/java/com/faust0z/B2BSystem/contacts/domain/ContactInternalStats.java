package com.faust0z.B2BSystem.contacts.domain;

public record ContactInternalStats(
        long total,
        long active,
        long expired
) {
}
