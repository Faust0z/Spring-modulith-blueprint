package com.faust0z.B2BSystem.tenant.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.registration.RegistrationInternalApi;
import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.application.TenantService;
import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faust0z.B2BSystem.tenant.application.TenantService;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
import com.faust0z.B2BSystem.tenant.web.TenantChangePlanRequest;
import com.faust0z.B2BSystem.tenant.web.TenantController;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private BillingCycleService billingCycleService;

    @Mock
    private RegistrationInternalApi registrationInternalApi;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private TenantController tenantController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc buildMockMvc() {
        return MockMvcBuilders.standaloneSetup(tenantController).build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private TenantResponse createDefaultTenantResponse(UUID id) {
        return new TenantResponse(
                id,
                "Red Hot Chili Arms",
                "30-12345678-9",
                Map.of("theme", "dark"),
                true,
                "red-hot-chili"
        );
    }

    @Nested
    class GetAllTenants {
        @Test
        void getAllTenants_TenantsExist_Returns200AndList() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            TenantResponse response = createDefaultTenantResponse(tenantId);
            when(tenantService.getAllTenants()).thenReturn(List.of(response));

            mockMvc.perform(get("/v1/tenants")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(tenantId.toString()))
                    .andExpect(jsonPath("$[0].name").value("Red Hot Chili Arms"))
                    .andExpect(jsonPath("$[0].taxId").value("30-12345678-9"));
        }
    }

    @Nested
    class GetTenantById {
        @Test
        void getTenantById_TenantExists_Returns200AndTenant() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            TenantResponse response = createDefaultTenantResponse(tenantId);
            when(tenantService.getTenantById(tenantId)).thenReturn(response);

            mockMvc.perform(get("/v1/tenants/{tenantId}", tenantId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(tenantId.toString()))
                    .andExpect(jsonPath("$.slug").value("red-hot-chili"));
        }
    }

    @Nested
    class CreateTenant {
        @Test
        void createTenant_ValidRequest_Returns201AndTenant() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            TenantCreateRequest request = new TenantCreateRequest("Red Hot Chili Arms", "30-12345678-9", "red-hot-chili", UUID.randomUUID());
            TenantResponse response = createDefaultTenantResponse(tenantId);
            when(tenantService.createTenant(any(TenantCreateRequest.class))).thenReturn(response);

            mockMvc.perform(post("/v1/tenants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(tenantId.toString()));
        }
    }

    @Nested
    class UpdateTenant {
        @Test
        void updateTenant_ValidRequest_Returns200AndUpdatedTenant() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            TenantUpdateRequest request = new TenantUpdateRequest("Acme Corp", "30-12345678-9", Map.of(), "acme-corp");
            TenantResponse response = createDefaultTenantResponse(tenantId);
            when(tenantService.updateTenant(eq(tenantId), any(TenantUpdateRequest.class))).thenReturn(response);

            mockMvc.perform(patch("/v1/tenants/{tenantId}", tenantId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.taxId").value("30-12345678-9"));
        }
    }

    @Nested
    class UpdateTenantTheme {
        @Test
        void updateTenantTheme_ValidContextAndRequest_Returns204NoContent() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID contextTenantId = UUID.randomUUID();
            TenantContext.setTenantId(contextTenantId);
            TenantUpdateThemeRequest request = new TenantUpdateThemeRequest("#FFFFFF", "#000000", null, null);

            doNothing().when(tenantService).updateTheme(any(TenantUpdateThemeRequest.class), eq(contextTenantId));

            mockMvc.perform(patch("/v1/tenants/me/theme")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(tenantService).updateTheme(any(TenantUpdateThemeRequest.class), eq(contextTenantId));
        }
    }


    @Nested
    class ChangePlan {
        @Test
        void changePlan_ValidContextAndRequest_Returns200AndUpdatedCycle() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID contextTenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            TenantContext.setTenantId(contextTenantId);
            TenantChangePlanRequest request = new TenantChangePlanRequest(planId);
            BillingCycleResponse response = new BillingCycleResponse(UUID.randomUUID(), contextTenantId, planId, null,
                    Instant.now(), Instant.now().plusSeconds(3600), 0, 0, 20000, CycleStatus.PENDING_PAYMENT, false);

            when(billingCycleService.changePlan(eq(contextTenantId), any(TenantChangePlanRequest.class))).thenReturn(response);

            mockMvc.perform(patch("/v1/tenants/me/plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.planId").value(planId.toString()));

            verify(billingCycleService).changePlan(eq(contextTenantId), any(TenantChangePlanRequest.class));
        }
    }

    @Nested
    class ToggleTenantStatus {
        @Test
        void toggleTenantStatus_TenantExists_Returns200AndUpdatedTenant() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            TenantResponse response = createDefaultTenantResponse(tenantId);
            when(tenantService.toggleTenantStatus(tenantId)).thenReturn(response);

            mockMvc.perform(patch("/v1/tenants/{tenantId}/status", tenantId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(true));
        }
    }

    @Nested
    class ResendEmailVerification {
        @Test
        void resend_ValidContext_CallsResendService() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID contextTenantId = UUID.randomUUID();
            TenantContext.setTenantId(contextTenantId);

            mockMvc.perform(post("/v1/tenants/me/email-verification/resend")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(registrationInternalApi).resendVerificationEmail(contextTenantId);
        }
    }
}
