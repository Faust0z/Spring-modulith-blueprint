package com.faust0z.B2BSystem.publics;

import com.faust0z.B2BSystem.core.ClientIpResolver;
import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
import com.faust0z.B2BSystem.registration.EmailVerificationRequest;
import com.faust0z.B2BSystem.registration.RegistrationInternalApi;
import com.faust0z.B2BSystem.registration.TenantRegisterRequest;
import com.faust0z.B2BSystem.tenant.BillingCycleHydratedResponse;
import com.faust0z.B2BSystem.tenant.BillingCycleInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.faust0z.B2BSystem.publics.TenantPublicResponse;
import com.faust0z.B2BSystem.publics.TenantPublicController;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/public/tenant")
@Tag(name = "Public Tenant", description = "Public tenant-level endpoints (no auth required)")
public class TenantPublicController {

    private final TenantInternalApi tenantInternalApi;
    private final BillingCycleInternalApi billingCycleInternalApi;
    private final RegistrationInternalApi registrationInternalApi;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "Public tenant info with visual theme")
    @ApiResponse(responseCode = "200", description = "Tenant info found successfully")
    @GetMapping("/{slug}")
    TenantPublicResponse getTenantInfo(@PathVariable String slug, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, s-maxage=604800000, stale-while-revalidate=605800000");

        UUID tenantId = tenantInternalApi.getTenantBySlug(slug).id();
        TenantContext.setTenantId(tenantId);
        try {
            log.info("Public getting tenant info | tenantId={}", tenantId);
            TenantResponse tenant = tenantInternalApi.getTenantById(tenantId);
            @SuppressWarnings("unchecked")
            Map<String, Object> theme = (Map<String, Object>) tenant.settings().get("theme");
            return new TenantPublicResponse(tenant.name(), theme != null ? theme : Map.of());
        } finally {
            TenantContext.clear();
        }
    }

    @Operation(summary = "Public endpoint to compute the next payment cycle of the business via username or email")
    @ApiResponse(responseCode = "200", description = "Payment status found successfully")
    @GetMapping("/compute-next-cycle/{identifier}")
    BillingCycleHydratedResponse computeNextCycleToPay(@PathVariable String identifier) {
        TenantInternalResponse tenant = tenantInternalApi.getTenantByUserIdentifier(identifier);
        TenantContext.setTenantId(tenant.id());
        try {
            log.info("Public computed tenant's next payment cycle | tenantId={}", tenant.id());
            return billingCycleInternalApi.computeNextCycleToPay(tenant);
        } finally {
            TenantContext.clear();
        }
    }

    @Operation(summary = "Register a new gym in one step, auto-login and send the email verification link")
    @ApiResponse(responseCode = "201", description = "Tenant and admin created, logged in, verification email sent")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserLoginInternalResponse register(@Valid @RequestBody TenantRegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        rateLimiterService.checkLimit("register:ip:" + clientIp, 5, 1, Duration.ofHours(1));
        rateLimiterService.checkLimit("register:email:" + request.email().trim().toLowerCase(), 2, 1, Duration.ofHours(1));

        UserAuthResponse authResult = registrationInternalApi.register(request);

        ResponseCookie jwtCookie = ResponseCookie.from("access_token", authResult.token())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(86400)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return authResult.response();
    }

    @Operation(summary = "Validate a tenant email via the magic link token")
    @ApiResponse(responseCode = "200", description = "Email validated successfully")
    @PostMapping("/register/validate-email")
    @ResponseStatus(HttpStatus.OK)
    void validateEmail(@Valid @RequestBody EmailVerificationRequest request, HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        rateLimiterService.checkLimit("validate-email:ip:" + clientIp, 5, 1, Duration.ofMinutes(15));
        registrationInternalApi.validateEmail(request.token());
    }
}
