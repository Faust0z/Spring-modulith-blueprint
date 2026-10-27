package com.faust0z.B2BSystem.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import com.faust0z.B2BSystem.auth.ClientPortalLoginResponse;
@Schema(description = "Sesión del portal del inquilino")
public record ClientPortalLoginResponse(
        UUID clientId,
        String fullName,
        UUID tenantId,
        boolean isOwner,
        boolean isRenter
) {
}
