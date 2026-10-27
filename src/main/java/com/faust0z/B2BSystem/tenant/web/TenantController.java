package com.faust0z.B2BSystem.tenant.web;

import com.faust0z.B2BSystem.core.RequireAdmin;
import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.registration.RegistrationInternalApi;
import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import com.faust0z.B2BSystem.tenant.application.TenantService;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
import com.faust0z.B2BSystem.tenant.web.TenantChangePlanRequest;
import com.faust0z.B2BSystem.tenant.web.TenantController;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant management")
public class TenantController {

    private final TenantService tenantService;
    private final BillingCycleService billingCycleService;
    private final RegistrationInternalApi registrationInternalApi;
    private final RateLimiterService rateLimiterService;

    @Operation(summary = "List all tenants")
    @ApiResponse(responseCode = "200", description = "Tenants retrieved successfully")
    @RequireAdmin
    @GetMapping
    List<TenantResponse> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @Operation(summary = "Get a single tenant by ID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Negocio encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "Negocio no encontrado")
    })
    @RequireAdmin
    @GetMapping("/{tenantId}")
    TenantResponse getTenantById(@PathVariable UUID tenantId) {
        return tenantService.getTenantById(tenantId);
    }

    @Operation(summary = "Create a new tenant. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Negocio creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Entrada invalida")
    })
    @RequireAdmin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TenantResponse createTenant(@Valid @RequestBody TenantCreateRequest request) {
        return tenantService.createTenant(request);
    }

    @Operation(summary = "Patch the specified tenant's data. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Negocio actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida"),
            @ApiResponse(responseCode = "404", description = "Negocio no encontrado")
    })
    @RequireAdmin
    @PatchMapping("/{tenantId}")
    TenantResponse updateTenant(@PathVariable UUID tenantId, @Valid @RequestBody TenantUpdateRequest request) {
        return tenantService.updateTenant(tenantId, request);
    }

    @Operation(summary = "Patch the specified tenant's theme. Requires Tenant_admin role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tema del negocio actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "La entrada no es un tema válido"),
            @ApiResponse(responseCode = "404", description = "Negocio no encontrado")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/me/theme")
    void updateTenant(@Valid @RequestBody TenantUpdateThemeRequest request) {
        UUID contextTenantId = TenantContext.getTenantId();
        tenantService.updateTheme(request, contextTenantId);
    }

    @Operation(summary = "Change the tenant's subscription plan (voluntary downgrade). Requires Tenant_admin role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan del negocio actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Plan o ciclo de facturación no encontrado")
    })
    @PatchMapping("/me/plan")
    BillingCycleResponse changePlan(@Valid @RequestBody TenantChangePlanRequest request) {
        UUID contextTenantId = TenantContext.getTenantId();
        return billingCycleService.changePlan(contextTenantId, request);
    }

    @Operation(summary = "Disable a tenant (Soft Delete). Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Estado del negocio actualizado correctamente")
    @RequireAdmin
    @PatchMapping("/{tenantId}/status")
    TenantResponse toggleTenantStatus(@PathVariable UUID tenantId) {
        return tenantService.toggleTenantStatus(tenantId);
    }

    @Operation(summary = "Resend the email confirmation magic link. Requires Tenant_admin role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Correo de confirmación reenviado correctamente")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/email-verification/resend")
    void resendEmailVerification() {
        UUID contextTenantId = TenantContext.getTenantId();
        rateLimiterService.checkLimit("resend-verification-tenant:" + contextTenantId, 3, 1, Duration.ofMinutes(15));
        registrationInternalApi.resendVerificationEmail(contextTenantId);
    }
}
