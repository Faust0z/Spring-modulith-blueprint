package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.tenant.BillingCycleInternalApi;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.domain.TenantLoginDisabledException;
import com.faust0z.B2BSystem.tenant.domain.TenantPeriodExpiredException;
import com.faust0z.B2BSystem.user.UserLoginDisabledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.AuthService;
import com.faust0z.B2BSystem.auth.CustomUserDetails;
import com.faust0z.B2BSystem.auth.JwtTokenProvider;
import com.faust0z.B2BSystem.auth.UserLoginRequest;
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserInternalApi userInternalApi;
    @Mock
    private TenantInternalApi tenantInternalApi;
    @Mock
    private BillingCycleInternalApi billingCycleInternalApi;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Nested
    class Login {

        @Test
        void login_AdminCredentials_ReturnsAuthResultWithoutTenant() {
            String identifier = "admin_user";
            UserLoginRequest request = new UserLoginRequest(identifier, "password");
            UserAuthInternalResponse authResponse = createAuthResponse(identifier, Role.ADMIN, null);
            CustomUserDetails userDetails = new CustomUserDetails(authResponse);
            Authentication authentication = mock(Authentication.class);
            UserResponse userResponse = createUserResponse(authResponse.id(), null, Role.ADMIN, true);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userInternalApi.getUserResponseByIdentifier(identifier)).thenReturn(userResponse);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token-123");

            UserAuthResponse result = authService.login(request);

            assertThat(result.token()).isEqualTo("jwt-token-123");
            assertThat(result.response().user()).isEqualTo(userResponse);
            assertThat(result.response().tenant()).isNull();
            verifyNoInteractions(tenantInternalApi, billingCycleInternalApi);
        }

        @Test
        void login_RegularUserActiveTenant_ReturnsAuthResult() {
            String identifier = "user@tenant.com";
            UUID tenantId = UUID.randomUUID();
            UserLoginRequest request = new UserLoginRequest(identifier, "password");
            UserAuthInternalResponse authResponse = createAuthResponse(identifier, Role.TENANT_USER, tenantId);
            CustomUserDetails userDetails = new CustomUserDetails(authResponse);
            Authentication authentication = mock(Authentication.class);
            UserResponse userResponse = createUserResponse(authResponse.id(), tenantId, Role.TENANT_USER, true);
            TenantResponse tenantResponse = createTenantResponse(tenantId, true);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userInternalApi.getUserResponseByIdentifier(identifier)).thenReturn(userResponse);
            when(tenantInternalApi.getTenantById(tenantId)).thenReturn(tenantResponse);
            when(billingCycleInternalApi.hasValidCycle()).thenReturn(true);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token-456");

            UserAuthResponse result = authService.login(request);

            assertThat(result.token()).isEqualTo("jwt-token-456");
            assertThat(result.response().user()).isEqualTo(userResponse);
            assertThat(result.response().tenant()).isEqualTo(tenantResponse);
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        void login_InactiveUser_ThrowsUserLoginDisabledException() {
            String identifier = "user@tenant.com";
            UUID tenantId = UUID.randomUUID();
            UserLoginRequest request = new UserLoginRequest(identifier, "password");
            UserAuthInternalResponse authResponse = createAuthResponse(identifier, Role.TENANT_USER, tenantId);
            CustomUserDetails userDetails = new CustomUserDetails(authResponse);
            Authentication authentication = mock(Authentication.class);
            UserResponse userResponse = createUserResponse(authResponse.id(), tenantId, Role.TENANT_USER, false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userInternalApi.getUserResponseByIdentifier(identifier)).thenReturn(userResponse);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UserLoginDisabledException.class);
            verifyNoInteractions(jwtTokenProvider, tenantInternalApi, billingCycleInternalApi);
        }

        @Test
        void login_InactiveTenant_ThrowsTenantDisabledException() {
            String identifier = "user@tenant.com";
            UUID tenantId = UUID.randomUUID();
            UserLoginRequest request = new UserLoginRequest(identifier, "password");
            UserAuthInternalResponse authResponse = createAuthResponse(identifier, Role.TENANT_USER, tenantId);
            CustomUserDetails userDetails = new CustomUserDetails(authResponse);
            Authentication authentication = mock(Authentication.class);
            UserResponse userResponse = createUserResponse(authResponse.id(), tenantId, Role.TENANT_USER, true);
            TenantResponse tenantResponse = createTenantResponse(tenantId, false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userInternalApi.getUserResponseByIdentifier(identifier)).thenReturn(userResponse);
            when(tenantInternalApi.getTenantById(tenantId)).thenReturn(tenantResponse);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(TenantLoginDisabledException.class);
            verifyNoInteractions(jwtTokenProvider, billingCycleInternalApi);
        }

        @Test
        void login_TenantPeriodExpired_ThrowsTenantPeriodExpiredExceptionAndClearsContext() {
            String identifier = "user@tenant.com";
            UUID tenantId = UUID.randomUUID();
            UserLoginRequest request = new UserLoginRequest(identifier, "password");
            UserAuthInternalResponse authResponse = createAuthResponse(identifier, Role.TENANT_USER, tenantId);
            CustomUserDetails userDetails = new CustomUserDetails(authResponse);
            Authentication authentication = mock(Authentication.class);
            UserResponse userResponse = createUserResponse(authResponse.id(), tenantId, Role.TENANT_USER, true);
            TenantResponse tenantResponse = createTenantResponse(tenantId, true);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userInternalApi.getUserResponseByIdentifier(identifier)).thenReturn(userResponse);
            when(tenantInternalApi.getTenantById(tenantId)).thenReturn(tenantResponse);
            when(billingCycleInternalApi.hasValidCycle()).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(TenantPeriodExpiredException.class);
            verifyNoInteractions(jwtTokenProvider);
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        void login_InvalidCredentials_ThrowsBadCredentialsException() {
            UserLoginRequest request = new UserLoginRequest("wrong", "bad-pass");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
            verifyNoInteractions(userInternalApi, tenantInternalApi, billingCycleInternalApi, jwtTokenProvider);
        }
    }

    private UserAuthInternalResponse createAuthResponse(String identifier, Role role, UUID tenantId) {
        return new UserAuthInternalResponse(UUID.randomUUID(), tenantId, identifier, "encoded", role, true);
    }

    private UserResponse createUserResponse(UUID id, UUID tenantId, Role role, boolean isActive) {
        return new UserResponse(id, tenantId, "John Doe", "test@test.com", "username123",
                role, isActive);
    }

    private TenantResponse createTenantResponse(UUID tenantId, boolean isActive) {
        return new TenantResponse(tenantId, "Acme", "123", Map.of(), isActive, "tenant");
    }
}