package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.catalog.ServicePlanInternalApi;
import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.tenant.TenantEmailVerificationEvent;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.auth.AuthInternalApi;
import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginRequest;
import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.user.EmailAlreadyInUseException;
import com.faust0z.B2BSystem.user.UserCreateRequest;
import com.faust0z.B2BSystem.user.UsernameAlreadyInUseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.faust0z.B2BSystem.registration.RegistrationInternalApi;
import com.faust0z.B2BSystem.registration.EmailVerificationTokenService;
import com.faust0z.B2BSystem.registration.RegistrationService;
import com.faust0z.B2BSystem.registration.TenantRegisterRequest;
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService implements RegistrationInternalApi {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final String ADMIN_PIN = "123456";

    private final TenantInternalApi tenantInternalApi;
    private final UserInternalApi userInternalApi;
    private final LocationInternalApi branchInternalApi;
    private final ServicePlanInternalApi servicePlanInternalApi;
    private final AuthInternalApi authInternalApi;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public UserAuthResponse register(TenantRegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userInternalApi.existsByEmail(email)) throw new EmailAlreadyInUseException();
        if (request.username() != null && userInternalApi.existsByUsername(request.username())) throw new UsernameAlreadyInUseException();
        tenantInternalApi.assertGymPlanAvailable(request.planId());

        TenantResponse tenant = tenantInternalApi.createTenant(new TenantCreateRequest(request.tenantName(), null, null,
                request.planId()));

        TenantContext.setTenantId(tenant.id());
        try {
            branchInternalApi.createDefaultLocation();
            servicePlanInternalApi.createDefaultServicePlan();
        } finally {
            TenantContext.clear();
        }

        UserResponse user = userInternalApi.createUser(new UserCreateRequest(request.tenantFullName(), email, request.username(),
                request.password(), Role.TENANT_ADMIN, ADMIN_PIN), tenant.id(), Role.ADMIN);

        tenantInternalApi.updateSettings(tenant.id(), Map.of("adminUserId", user.id()));

        String token = emailVerificationTokenService.issue(user.id(), TOKEN_VALIDITY);
        eventPublisher.publishEvent(new TenantEmailVerificationEvent(email, token));

        UserAuthResponse auth = authInternalApi.login(
                new UserLoginRequest(request.username() != null ? request.username() : email, request.password()));

        log.info("Registration completed | tenantId={} | userId={} | email={}", tenant.id(), user.id(), email);
        return auth;
    }

    @Transactional
    @Override
    public void validateEmail(String token) {
        UUID userId = emailVerificationTokenService.resolve(token);
        UUID tenantId = userInternalApi.getTenantIdByUserId(userId);
        tenantInternalApi.updateSettings(tenantId, Map.of("validated_email", Boolean.TRUE));
        emailVerificationTokenService.consume(token);
        log.info("Email validated | tenantId={} | userId={}", tenantId, userId);
    }

    @Transactional
    @Override
    public void resendVerificationEmail(UUID tenantId) {
        Map<String, Object> settings = tenantInternalApi.getTenantSettings(tenantId);
        Object rawUserId = settings.get("adminUserId");
        if (rawUserId == null) {
            log.error("No admin user on file for resend | tenantId={}", tenantId);
            return;
        }
        UUID adminUserId = rawUserId instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(rawUserId));
        UserResponse admin = userInternalApi.getUserResponseById(adminUserId);

        String token = emailVerificationTokenService.issue(adminUserId, TOKEN_VALIDITY);
        eventPublisher.publishEvent(new TenantEmailVerificationEvent(admin.email(), token));
        log.info("Verification email resent | tenantId={} | userId={}", tenantId, adminUserId);
    }
}