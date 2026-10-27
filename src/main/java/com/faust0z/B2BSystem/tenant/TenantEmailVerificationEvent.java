package com.faust0z.B2BSystem.tenant;

import com.faust0z.B2BSystem.tenant.TenantEmailVerificationEvent;
public record TenantEmailVerificationEvent(
        String email,
        String token
) {
}