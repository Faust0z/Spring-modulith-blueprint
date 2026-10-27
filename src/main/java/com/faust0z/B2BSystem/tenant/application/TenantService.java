package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.tenant.application.BillingDemoCycleService;
import com.faust0z.B2BSystem.tenant.application.TenantMapper;
import com.faust0z.B2BSystem.tenant.domain.CuitAlreadyInUseException;
import com.faust0z.B2BSystem.tenant.domain.SlugAlreadyInUseException;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.domain.TenantDisabledException;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.domain.TenantNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.TenantRepository;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
import com.faust0z.B2BSystem.tenant.application.BillingDemoCycleService;
import com.faust0z.B2BSystem.tenant.application.TenantMapper;
import com.faust0z.B2BSystem.tenant.application.TenantService;
import com.faust0z.B2BSystem.tenant.domain.CuitAlreadyInUseException;
import com.faust0z.B2BSystem.tenant.domain.SlugAlreadyInUseException;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.domain.TenantDisabledException;
import com.faust0z.B2BSystem.tenant.domain.TenantNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.TenantRepository;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService implements TenantInternalApi {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final CacheManager cacheManager;
    private final UserInternalApi userInternalApi;
    private final BillingDemoCycleService billingDemoCycleService;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        log.debug("Fetching tenants");
        return tenantRepository.findAllBy(TenantResponse.class);
    }

    @Cacheable(value = "tenant", key = "#tenantId")
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID tenantId) {
        log.debug("Fetching tenant by id: {}", tenantId);
        return tenantRepository.findById(tenantId, TenantResponse.class).orElseThrow(TenantNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public TenantInternalResponse getTenantByUserIdentifier(String identifier) {
        UserResponse userResponse = userInternalApi.getUserResponseByIdentifier(identifier);
        return tenantRepository.findById(userResponse.tenantId(), TenantInternalResponse.class).orElseThrow(TenantNotFoundException::new);
    }

    @Transactional
    @Override
    public TenantResponse createTenant(TenantCreateRequest request) {
        log.debug("Processing registration for tenant: {}", request.name());

        if (request.taxId() != null && tenantRepository.existsByTaxId(request.taxId())) throw new CuitAlreadyInUseException();

        String targetSlug = request.slug() != null && !request.slug().isBlank() ? request.slug() : generateSlug(request.name());

        if (tenantRepository.existsBySlug(targetSlug)) throw new SlugAlreadyInUseException();

        Tenant tenantEntity = tenantMapper.toEntity(request);
        tenantEntity.setSlug(targetSlug);

        tenantRepository.save(tenantEntity);

        try {
            TenantContext.setTenantId(tenantEntity.getId());
            billingDemoCycleService.createDemoCycle(tenantEntity.getId(), request.planId());
        } finally {
            TenantContext.clear();
        }

        return tenantMapper.toResponse(tenantEntity);
    }

    @Transactional(readOnly = true)
    @Override
    public void assertGymPlanAvailable(UUID planId) {
        SubscriptionPlanEntity plan = subscriptionPlanRepository.findById(planId).orElseThrow(SubscriptionPlanNotFoundException::new);
        if (!Boolean.TRUE.equals(plan.getIsActive())) throw new SubscriptionPlanNotFoundException();
    }

    private String generateSlug(String input) {
        String slug = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug.substring(0, Math.min(slug.length(), 30));
    }

    @Caching(
            put = @CachePut(value = "tenant", key = "#tenantId"),
            evict = {
                    @CacheEvict(value = "tenant_settings", key = "#tenantId"),
                    @CacheEvict(value = "tenant_active_validation", allEntries = true)
            }
    )
    @Transactional
    public TenantResponse updateTenant(UUID tenantId, TenantUpdateRequest request) {
        log.debug("Updating tenant | tenantId: {}", tenantId);

        Tenant existingTenant = tenantRepository.findById(tenantId).orElseThrow(TenantNotFoundException::new);

        if (request.taxId() != null && !existingTenant.getTaxId().equalsIgnoreCase(request.taxId())) {
            if (tenantRepository.existsByTaxId(request.taxId())) throw new CuitAlreadyInUseException();
        }

        if (request.slug() != null && !existingTenant.getSlug().equalsIgnoreCase(request.slug())) {
            if (tenantRepository.existsBySlug(request.slug())) throw new SlugAlreadyInUseException();
        }

        tenantMapper.updateTenantFromDto(request, existingTenant);

        Cache cache = cacheManager.getCache("revocations");
        if (cache != null) cache.put("tenant:" + tenantId, Instant.now().getEpochSecond());

        return tenantMapper.toResponse(existingTenant);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "tenant", key = "#tenantId"),
                    @CacheEvict(value = "tenant_settings", key = "#tenantId")
            }
    )
    @Transactional
    @Override
    public void updateTheme(TenantUpdateThemeRequest request, UUID tenantId) {
        log.debug("Updating tenant theme | custom={} | schema={} | palette={} | tenantId={}", request.isCustom(), request.schema(), request.palette(), tenantId);

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(TenantNotFoundException::new);

        Map<String, Object> settings = new HashMap<>(tenant.getSettings());
        Map<String, Object> themeMap = new HashMap<>();

        if (request.isCustom()) {
            themeMap.put("bg", request.bg());
            themeMap.put("accent", request.accent());
        } else {
            themeMap.put("schema", request.schema());
            themeMap.put("palette", request.palette());
        }

        settings.put("theme", themeMap);
        tenant.setSettings(settings);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "tenant", key = "#tenantId"),
                    @CacheEvict(value = "tenant_settings", key = "#tenantId"),
                    @CacheEvict(value = "tenant_active_validation", allEntries = true)
            }
    )
    @Transactional
    public TenantResponse toggleTenantStatus(UUID tenantId) {
        log.debug("Updating tenant status | tenantId: {}", tenantId);

        Tenant tenantEntity = tenantRepository.findById(tenantId).orElseThrow(TenantNotFoundException::new);

        tenantEntity.setIsActive(!tenantEntity.getIsActive());

        Cache cache = cacheManager.getCache("revocations");
        if (cache != null) cache.put("tenant:" + tenantId, Instant.now().getEpochSecond());

        log.info("Tenant status toggled | newStatus={}", tenantEntity.getIsActive());
        return tenantMapper.toResponse(tenantEntity);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "tenant", key = "#tenantId"),
                    @CacheEvict(value = "tenant_settings", key = "#tenantId")
            }
    )
    @Transactional
    @Override
    public void updateSettings(UUID tenantId, Map<String, Object> updates) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(TenantNotFoundException::new);

        Map<String, Object> settings = new HashMap<>(tenant.getSettings());
        settings.putAll(updates);
        tenant.setSettings(settings);
        log.info("Tenant settings updated | tenantId={} | keys={}", tenantId, updates.keySet());
    }

    @Cacheable(value = "tenant_settings", key = "#tenantId")
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTenantSettings(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(TenantNotFoundException::new)
                .getSettings();
    }

    @Override
    @Cacheable(value = "tenant_active_validation", key = "#slug")
    @Transactional(readOnly = true)
    public TenantInternalResponse getTenantBySlug(String slug) {
        TenantInternalResponse response = tenantRepository.findBySlug(slug, TenantInternalResponse.class).orElseThrow(TenantNotFoundException::new);
        if (!response.isActive()) throw new TenantDisabledException();
        return response;
    }
}
