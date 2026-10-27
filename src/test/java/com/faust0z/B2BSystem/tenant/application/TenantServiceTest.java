package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.application.BillingDemoCycleService;
import com.faust0z.B2BSystem.tenant.application.TenantMapper;
import com.faust0z.B2BSystem.tenant.domain.CuitAlreadyInUseException;
import com.faust0z.B2BSystem.tenant.domain.SlugAlreadyInUseException;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.domain.TenantNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.infrastructure.TenantRepository;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
import com.faust0z.B2BSystem.tenant.application.TenantService;
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private BillingDemoCycleService billingDemoCycleService;
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private TenantService tenantService;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    private TenantResponse createDefaultResponse(UUID id) {
        return new TenantResponse(id, "Red Hot Chili Arms", "30-11112222-3", Map.of("theme", "light"), true,
                "tenant");
    }

    @Nested
    class GetAllTenants {

        @Test
        void getAllTenants_TenantsExist_ReturnsTenantList() {
            UUID tenantId = UUID.randomUUID();
            TenantResponse response = createDefaultResponse(tenantId);
            when(tenantRepository.findAllBy(TenantResponse.class)).thenReturn(List.of(response));

            List<TenantResponse> result = tenantService.getAllTenants();

            assertThat(result).containsExactly(response);
            verify(tenantRepository).findAllBy(TenantResponse.class);
        }
    }

    @Nested
    class GetTenantById {

        @Test
        void getTenantById_Exists_ReturnsResponse() {
            UUID tenantId = UUID.randomUUID();
            TenantResponse response = createDefaultResponse(tenantId);
            when(tenantRepository.findById(tenantId, TenantResponse.class)).thenReturn(Optional.of(response));

            TenantResponse result = tenantService.getTenantById(tenantId);

            assertThat(result).isEqualTo(response);
        }

        @Test
        void getTenantById_NotExists_ThrowsTenantNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            when(tenantRepository.findById(tenantId, TenantResponse.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getTenantById(tenantId))
                    .isInstanceOf(TenantNotFoundException.class);
        }
    }

    @Nested
    class CreateTenant {

        @Test
        void createTenant_ValidData_SavesCreatesDemoAndReturnsResponse() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            TenantCreateRequest request = new TenantCreateRequest("Gym", "30-11112222-3", "gym-slug", planId);
            Tenant entity = new Tenant();
            entity.setId(tenantId);
            TenantResponse response = createDefaultResponse(tenantId);

            when(tenantRepository.existsByTaxId(request.taxId())).thenReturn(false);
            when(tenantRepository.existsBySlug(request.slug())).thenReturn(false);
            when(tenantMapper.toEntity(request)).thenReturn(entity);
            when(tenantMapper.toResponse(entity)).thenReturn(response);

            TenantResponse result = tenantService.createTenant(request);

            assertThat(result).isEqualTo(response);
            assertThat(entity.getSlug()).isEqualTo(request.slug());
            verify(tenantRepository).save(entity);
            verify(billingDemoCycleService).createDemoCycle(tenantId, planId);
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        void createTenant_MissingSlugGeneratesTarget_SavesCreatesDemoAndReturnsResponse() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            TenantCreateRequest request = new TenantCreateRequest("Gym Nuevo", "30-11112222-3", null, planId);
            Tenant entity = new Tenant();
            entity.setId(tenantId);
            TenantResponse response = createDefaultResponse(tenantId);

            when(tenantRepository.existsByTaxId(request.taxId())).thenReturn(false);
            when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
            when(tenantMapper.toEntity(request)).thenReturn(entity);
            when(tenantMapper.toResponse(entity)).thenReturn(response);

            TenantResponse result = tenantService.createTenant(request);

            assertThat(result).isEqualTo(response);
            assertThat(entity.getSlug()).isNotNull().isNotEqualTo("");
            verify(tenantRepository).save(entity);
            verify(billingDemoCycleService).createDemoCycle(tenantId, planId);
        }

        @Test
        void createTenant_TaxIdExists_ThrowsCuitAlreadyInUseException() {
            TenantCreateRequest request = new TenantCreateRequest("Gym Nuevo", "30-11112222-3", "tenant", UUID.randomUUID());
            when(tenantRepository.existsByTaxId(request.taxId())).thenReturn(true);

            assertThatThrownBy(() -> tenantService.createTenant(request))
                    .isInstanceOf(CuitAlreadyInUseException.class);

            verifyNoInteractions(tenantMapper, billingDemoCycleService);
            verify(tenantRepository, never()).save(any());
        }

        @Test
        void createTenant_SlugExists_ThrowsSlugAlreadyInUseException() {
            TenantCreateRequest request = new TenantCreateRequest("Gym Nuevo", "30-11112222-3", "gym-slug", UUID.randomUUID());
            when(tenantRepository.existsByTaxId(request.taxId())).thenReturn(false);
            when(tenantRepository.existsBySlug(request.slug())).thenReturn(true);

            assertThatThrownBy(() -> tenantService.createTenant(request))
                    .isInstanceOf(SlugAlreadyInUseException.class);

            verifyNoInteractions(tenantMapper, billingDemoCycleService);
            verify(tenantRepository, never()).save(any());
        }

        @Test
        void createTenant_DemoCycleFails_PropagatesExceptionAndClearsContext() {
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            TenantCreateRequest request = new TenantCreateRequest("Gym", "30-11112222-3", "gym-slug", planId);
            Tenant entity = new Tenant();
            entity.setId(tenantId);

            when(tenantRepository.existsByTaxId(request.taxId())).thenReturn(false);
            when(tenantRepository.existsBySlug(request.slug())).thenReturn(false);
            when(tenantMapper.toEntity(request)).thenReturn(entity);
            doThrow(new RuntimeException("Demo Service Unavailable")).when(billingDemoCycleService).createDemoCycle(tenantId, planId);

            assertThatThrownBy(() -> tenantService.createTenant(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Demo Service Unavailable");

            verify(tenantRepository).save(entity);
            assertThat(TenantContext.getTenantId()).isNull();
        }
    }

    @Nested
    class AssertGymPlanAvailable {

        @Test
        void activeGymPlan_DoesNotThrow() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
            plan.setId(planId);
            plan.setIsActive(true);
            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

            tenantService.assertGymPlanAvailable(planId);
        }

        @Test
        void planNotFound_ThrowsSubscriptionPlanNotFoundException() {
            UUID planId = UUID.randomUUID();
            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.assertGymPlanAvailable(planId))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);
        }

        @Test
        void inactivePlan_ThrowsSubscriptionPlanNotFoundException() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
            plan.setId(planId);
            plan.setIsActive(false);
            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> tenantService.assertGymPlanAvailable(planId))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);
        }

        @Test
        void activeNonGymPlan_DoesNotThrow() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
            plan.setId(planId);
            plan.setIsActive(true);
            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

            tenantService.assertGymPlanAvailable(planId);
        }
    }

    @Nested
    class UpdateTenant {

        @Test
        void updateTenant_TaxIdUnchanged_UpdatesAndReturnsResponse() {
            UUID tenantId = UUID.randomUUID();
            TenantUpdateRequest request = new TenantUpdateRequest("Red Hot Chili Arms", "30-11112222-3", Map.of(), "newSlug");
            Tenant existingEntity = new Tenant();
            existingEntity.setTaxId("30-11112222-3");
            existingEntity.setSlug("oldSlug");
            TenantResponse response = createDefaultResponse(tenantId);
            Cache cacheMock = mock(Cache.class);

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingEntity));
            when(tenantRepository.existsBySlug(request.slug())).thenReturn(false);
            when(cacheManager.getCache("revocations")).thenReturn(cacheMock);
            when(tenantMapper.toResponse(existingEntity)).thenReturn(response);

            TenantResponse result = tenantService.updateTenant(tenantId, request);

            assertThat(result).isEqualTo(response);
            verify(tenantRepository, never()).existsByTaxId(anyString());
            verify(tenantMapper).updateTenantFromDto(request, existingEntity);
            verify(cacheMock).put(eq("tenant:" + tenantId), any(Long.class));
        }

        @Test
        void updateTenant_TaxIdChangedAndExists_ThrowsCuitAlreadyInUseException() {
            UUID tenantId = UUID.randomUUID();
            TenantUpdateRequest request = new TenantUpdateRequest("Gym Modificado", "30-98765432-1", Map.of(), "tenant");
            Tenant existingEntity = new Tenant();
            existingEntity.setTaxId("30-11112222-3");

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingEntity));
            when(tenantRepository.existsByTaxId(request.taxId())).thenReturn(true);

            assertThatThrownBy(() -> tenantService.updateTenant(tenantId, request))
                    .isInstanceOf(CuitAlreadyInUseException.class);

            verifyNoInteractions(cacheManager, tenantMapper);
        }
    }


    @Nested
    class UpdateTheme {

        @Test
        void updateTheme_Custom_UpdatesNestedThemeAndPreservesSettings() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> initialSettings = new HashMap<>();
            initialSettings.put("timezone", "America/Argentina/Buenos_Aires");
            entity.setSettings(initialSettings);

            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest("#FFFFFF", "#BBBBBB", null, null);
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            tenantService.updateTheme(request, tenantId);

            Map<String, Object> savedSettings = entity.getSettings();
            assertThat(savedSettings).containsEntry("timezone", "America/Argentina/Buenos_Aires");

            @SuppressWarnings("unchecked")
            Map<String, Object> themeMap = (Map<String, Object>) savedSettings.get("theme");
            assertThat(themeMap)
                    .containsEntry("bg", "#FFFFFF")
                    .containsEntry("accent", "#BBBBBB");
        }

        @Test
        void updateTheme_Custom_OverridesPreviousTheme() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> initialSettings = new HashMap<>();
            Map<String, Object> oldTheme = new HashMap<>();
            oldTheme.put("schema", "dark");
            initialSettings.put("theme", oldTheme);
            initialSettings.put("timezone", "America/Argentina/Buenos_Aires");
            entity.setSettings(initialSettings);

            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest("#FFFFFF", "#000000", null, null);
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            tenantService.updateTheme(request, tenantId);

            @SuppressWarnings("unchecked")
            Map<String, Object> themeMap = (Map<String, Object>) entity.getSettings().get("theme");
            assertThat(themeMap)
                    .doesNotContainKey("schema")
                    .containsEntry("bg", "#FFFFFF")
                    .containsEntry("accent", "#000000");
            assertThat(entity.getSettings()).containsEntry("timezone", "America/Argentina/Buenos_Aires");
        }

        @Test
        void updateTheme_Schema_StoresSchemaKey() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> initialSettings = new HashMap<>();
            initialSettings.put("timezone", "America/Argentina/Buenos_Aires");
            entity.setSettings(initialSettings);

            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest(null, null, "dark", "forest");
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            tenantService.updateTheme(request, tenantId);

            @SuppressWarnings("unchecked")
            Map<String, Object> themeMap = (Map<String, Object>) entity.getSettings().get("theme");
            assertThat(themeMap)
                    .doesNotContainKeys("bg", "accent")
                    .containsEntry("schema", "dark")
                    .containsEntry("palette", "forest");
            assertThat(entity.getSettings()).containsEntry("timezone", "America/Argentina/Buenos_Aires");
        }

        @Test
        void updateTheme_Schema_OverridesPreviousCustomTheme() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> initialSettings = new HashMap<>();
            Map<String, Object> oldTheme = new HashMap<>();
            oldTheme.put("bg", "#FFFFFF");
            oldTheme.put("accent", "#000000");
            initialSettings.put("theme", oldTheme);
            entity.setSettings(initialSettings);

            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest(null, null, "light", "sage");
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            tenantService.updateTheme(request, tenantId);

            @SuppressWarnings("unchecked")
            Map<String, Object> themeMap = (Map<String, Object>) entity.getSettings().get("theme");
            assertThat(themeMap)
                    .doesNotContainKeys("bg", "accent")
                    .containsEntry("schema", "light")
                    .containsEntry("palette", "sage");
        }

        @Test
        void updateTheme_TenantNotFound_ThrowsTenantNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest("#FFFFFF", "#BBBBBB", null, null);
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.updateTheme(request, tenantId))
                    .isInstanceOf(TenantNotFoundException.class);
        }

        @Test
        void updateTheme_Palette_StoresPaletteKey() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> initialSettings = new HashMap<>();
            initialSettings.put("timezone", "America/Argentina/Buenos_Aires");
            entity.setSettings(initialSettings);

            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest(null, null, "dark", "forest");
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            tenantService.updateTheme(request, tenantId);

            @SuppressWarnings("unchecked")
            Map<String, Object> themeMap = (Map<String, Object>) entity.getSettings().get("theme");
            assertThat(themeMap)
                    .doesNotContainKeys("bg", "accent")
                    .containsEntry("schema", "dark")
                    .containsEntry("palette", "forest");
            assertThat(entity.getSettings()).containsEntry("timezone", "America/Argentina/Buenos_Aires");
        }

        @Test
        void updateTheme_Palette_OverridesPreviousCustomTheme() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> initialSettings = new HashMap<>();
            Map<String, Object> oldTheme = new HashMap<>();
            oldTheme.put("bg", "#FFFFFF");
            oldTheme.put("accent", "#000000");
            initialSettings.put("theme", oldTheme);
            entity.setSettings(initialSettings);

            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest(null, null, "light", "indigo");
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            tenantService.updateTheme(request, tenantId);

            @SuppressWarnings("unchecked")
            Map<String, Object> themeMap = (Map<String, Object>) entity.getSettings().get("theme");
            assertThat(themeMap)
                    .doesNotContainKeys("bg", "accent")
                    .containsEntry("schema", "light")
                    .containsEntry("palette", "indigo");
        }
    }

    @Nested
    class ToggleTenantStatus {

        @Test
        void toggleTenantStatus_Exists_TogglesAndSaves() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            entity.setIsActive(true);
            TenantResponse response = createDefaultResponse(tenantId);
            Cache cacheMock = mock(Cache.class);

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));
            when(tenantMapper.toResponse(entity)).thenReturn(response);
            when(cacheManager.getCache("revocations")).thenReturn(cacheMock);

            TenantResponse result = tenantService.toggleTenantStatus(tenantId);

            assertThat(entity.getIsActive()).isFalse();
            assertThat(result).isEqualTo(response);
            verify(cacheMock).put(eq("tenant:" + tenantId), any(Long.class));
        }

        @Test
        void toggleTenantStatus_NotExists_ThrowsTenantNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.toggleTenantStatus(tenantId))
                    .isInstanceOf(TenantNotFoundException.class);
        }
    }

    @Nested
    class GetTenantSettings {

        @Test
        void getTenantSettings_Exists_ReturnsSettingsMap() {
            UUID tenantId = UUID.randomUUID();
            Tenant entity = new Tenant();
            Map<String, Object> expectedSettings = Map.of("quota", 100);
            entity.setSettings(expectedSettings);
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

            Map<String, Object> result = tenantService.getTenantSettings(tenantId);

            assertThat(result).isEqualTo(expectedSettings);
        }

        @Test
        void getTenantSettings_NotExists_ThrowsTenantNotFoundException() {
            UUID tenantId = UUID.randomUUID();
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getTenantSettings(tenantId))
                    .isInstanceOf(TenantNotFoundException.class);
        }
    }
}
