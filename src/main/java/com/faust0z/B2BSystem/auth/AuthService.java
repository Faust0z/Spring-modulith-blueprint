package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.tenant.BillingCycleInternalApi;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.domain.TenantLoginDisabledException;
import com.faust0z.B2BSystem.tenant.domain.TenantPeriodExpiredException;
import com.faust0z.B2BSystem.user.UserLoginDisabledException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

import com.faust0z.B2BSystem.auth.AuthInternalApi;
import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
import com.faust0z.B2BSystem.auth.AuthService;
import com.faust0z.B2BSystem.auth.CustomUserDetails;
import com.faust0z.B2BSystem.auth.JwtTokenProvider;
import com.faust0z.B2BSystem.auth.UserLoginRequest;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthInternalApi {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserInternalApi userInternalApi;
    private final TenantInternalApi tenantInternalApi;
    private final BillingCycleInternalApi billingCycleInternalApi;

    @Override
    public UserAuthResponse login(UserLoginRequest loginRequest) {
        log.debug("Authenticating user | identifier={}", loginRequest.identifier());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.identifier(), loginRequest.password()));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        UserResponse userResponse = userInternalApi.getUserResponseByIdentifier(userDetails.getUsername());
        if (!userResponse.isActive()) throw new UserLoginDisabledException();

        TenantResponse tenantResponse = null;
        if (userResponse.tenantId() != null) {
            tenantResponse = tenantInternalApi.getTenantById(userResponse.tenantId());
            if (!tenantResponse.isActive()) throw new TenantLoginDisabledException();

            try {
                TenantContext.setTenantId(tenantResponse.id());
                if (!billingCycleInternalApi.hasValidCycle()) throw new TenantPeriodExpiredException();
            } finally {
                TenantContext.clear();
            }
        }

        String token = jwtTokenProvider.generateToken(userDetails);
        log.info("User authenticated | identifier={}", userDetails.getUsername());
        return new UserAuthResponse(token, new UserLoginInternalResponse(userResponse, tenantResponse));
    }

    public String authorizeAdminPin(UUID userId, String rawPin) {
        userInternalApi.validateAdminPin(userId, rawPin);
        return jwtTokenProvider.generatePinAuthorizationToken(userId);
    }

}