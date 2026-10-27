package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.user.UserService;
import com.faust0z.B2BSystem.user.UserChangeRoleRequest;
import com.faust0z.B2BSystem.user.UserController;
import com.faust0z.B2BSystem.user.UserCreateRequest;
import com.faust0z.B2BSystem.user.UserUpdatePasswordRequest;
import com.faust0z.B2BSystem.user.UserUpdateRequest;
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    class GetAllUsers {
        @Test
        void getAllUsers_ValidTenant_DelegatesToService() {
            UUID tenantId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            List<UserResponse> expected = List.of(createDefaultResponse());
            when(userService.getAllUsers(tenantId)).thenReturn(expected);

            List<UserResponse> result = userController.getAllUsers();

            assertThat(result).isEqualTo(expected);
            verify(userService).getAllUsers(tenantId);
        }
    }

    @Nested
    class GetUserById {
        @Test
        void getUserById_ValidId_DelegatesToService() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            UserResponse expected = createDefaultResponse();
            when(userService.getUserById(userId, tenantId)).thenReturn(expected);

            UserResponse result = userController.getUserById(userId);

            assertThat(result).isEqualTo(expected);
            verify(userService).getUserById(userId, tenantId);
        }
    }

    @Nested
    class GetMyDetails {
        @Test
        void getMyDetails_ValidJwt_ExtractsSubjectAndDelegates() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            UserResponse expected = createDefaultResponse();
            when(jwt.getSubject()).thenReturn(userId.toString());
            when(userService.getUserById(userId, tenantId)).thenReturn(expected);

            UserResponse result = userController.getMyDetails(jwt);

            assertThat(result).isEqualTo(expected);
            verify(userService).getUserById(userId, tenantId);
        }
    }

    @Nested
    class CreateUser {
        @Test
        void createUser_ValidRequestAndJwt_ExtractsRoleAndDelegates() {
            UUID tenantId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            UserCreateRequest request = new UserCreateRequest("John Doe", "test@test.com",
                    "user", "pass", Role.TENANT_USER, null);
            UserResponse expected = createDefaultResponse();
            when(jwt.getClaimAsString("role")).thenReturn("TENANT_ADMIN");
            when(userService.createUser(request, tenantId, Role.TENANT_ADMIN)).thenReturn(expected);

            UserResponse result = userController.createUser(request, jwt);

            assertThat(result).isEqualTo(expected);
            verify(userService).createUser(request, tenantId, Role.TENANT_ADMIN);
        }
    }

    @Nested
    class UpdateUser {
        @Test
        void updateUser_ValidRequest_DelegatesToService() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            UserUpdateRequest request = new UserUpdateRequest("John Doe", "new@test.com",
                    "newuser");
            UserResponse expected = createDefaultResponse();
            when(userService.updateUser(userId, tenantId, request)).thenReturn(expected);

            UserResponse result = userController.updateUser(userId, request);

            assertThat(result).isEqualTo(expected);
            verify(userService).updateUser(userId, tenantId, request);
        }
    }

    @Nested
    class UpdateMyPassword {
        @Test
        void updateMyPassword_ValidJwtAndRequest_ChecksRateLimitAndDelegates() {
            UUID userId = UUID.randomUUID();
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("old", "new");
            when(jwt.getSubject()).thenReturn(userId.toString());

            userController.updateMyPassword(jwt, request);

            verify(rateLimiterService).checkLimit(
                    "password-update:" + userId, 5, 1, Duration.ofMinutes(15)
            );
            verify(userService).updateMyPassword(userId, request);
        }
    }

    @Nested
    class UpdateUserRole {
        @Test
        void updateUserRole_ValidRequestAndJwt_ExtractsRoleAndDelegates() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            UserChangeRoleRequest request = new UserChangeRoleRequest(Role.TENANT_USER);
            UserResponse expected = createDefaultResponse();
            when(jwt.getClaimAsString("role")).thenReturn("ADMIN");
            when(userService.changeUserRole(userId, tenantId, request, Role.ADMIN)).thenReturn(expected);

            UserResponse result = userController.updateUserRole(userId, request, jwt);

            assertThat(result).isEqualTo(expected);
            verify(userService).changeUserRole(userId, tenantId, request, Role.ADMIN);
        }
    }

    @Nested
    class ToggleUserStatus {
        @Test
        void toggleUserStatus_ValidId_DelegatesToService() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);
            UserResponse expected = createDefaultResponse();
            when(userService.toggleUserStatus(userId, tenantId)).thenReturn(expected);

            UserResponse result = userController.toggleUserStatus(userId);

            assertThat(result).isEqualTo(expected);
            verify(userService).toggleUserStatus(userId, tenantId);
        }
    }

    private UserResponse createDefaultResponse() {
        return new UserResponse(UUID.randomUUID(), UUID.randomUUID(), "Juan Perez", "juan@gimnasio.com.ar",
                "user123", Role.TENANT_USER, true);
    }
}
