package com.faust0z.B2BSystem.tenant;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
import org.springframework.cache.annotation.Cacheable;

import java.util.Map;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
public interface TenantInternalApi {
    TenantResponse getTenantById(UUID tenantId);

    TenantInternalResponse getTenantBySlug(String slug);

    @Cacheable(value = "settings", key = "#tenantId")
    Map<String, Object> getTenantSettings(UUID tenantId);

    TenantInternalResponse getTenantByUserIdentifier(String identifier);

    TenantResponse createTenant(TenantCreateRequest request);

    void updateTheme(TenantUpdateThemeRequest request, UUID tenantId);

    void updateSettings(UUID tenantId, Map<String, Object> updates);

    void assertGymPlanAvailable(UUID planId);
}