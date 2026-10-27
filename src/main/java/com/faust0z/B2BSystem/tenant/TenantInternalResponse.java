package com.faust0z.B2BSystem.tenant;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
public record TenantInternalResponse(
        UUID id,
        String name,
        String taxId,
        boolean isActive,
        String slug
) {
}
