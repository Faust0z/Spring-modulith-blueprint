package com.faust0z.B2BSystem.auth;

import java.util.UUID;

import com.faust0z.B2BSystem.auth.ClientPortalCredential;
public record ClientPortalCredential(
        UUID id,
        UUID tenantId,
        String pinHash,
        String fullName,
        boolean isOwner,
        boolean isRenter,
        boolean isActive
) {
}
