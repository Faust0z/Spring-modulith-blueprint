package com.faust0z.B2BSystem.user;


import java.util.UUID;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.user.UserCreateRequest;
public interface UserInternalApi {
    UserResponse getUserResponseByIdentifier(String identifier);

    UserAuthInternalResponse getUserAuthInternalByIdentifier(String identifier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    UserResponse getUserResponseById(UUID userId);

    UUID getTenantIdByUserId(UUID userId);

    UserResponse createUser(UserCreateRequest request, UUID tenantId, Role requesterRole);

    UUID validateAdminPin(UUID userId, String adminPin);

    boolean existsByIdAndAdminPinNotNull(UUID userId);
}