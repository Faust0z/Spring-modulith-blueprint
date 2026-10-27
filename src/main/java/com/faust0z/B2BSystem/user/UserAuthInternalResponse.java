package com.faust0z.B2BSystem.user;

import java.util.UUID;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
public record UserAuthInternalResponse(
        UUID tenantId,
        UUID id,
        String email,
        String password,
        Role role,
        boolean isActive
) {
}