package com.faust0z.B2BSystem.publics;

import com.faust0z.B2BSystem.core.ClientIpResolver;
import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
import com.faust0z.B2BSystem.registration.EmailVerificationRequest;
import com.faust0z.B2BSystem.registration.RegistrationInternalApi;
import com.faust0z.B2BSystem.registration.TenantRegisterRequest;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faust0z.B2BSystem.publics.TenantPublicController;
@ExtendWith(MockitoExtension.class)
class TenantPublicControllerTest {

    @Mock
    private TenantInternalApi tenantInternalApi;
    @Mock
    private RegistrationInternalApi registrationInternalApi;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private ClientIpResolver clientIpResolver;

    @InjectMocks
    private TenantPublicController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    class GetTenantInfo {

        @Test
        void returnsNameLocationAndTheme() throws Exception {
            String slug = "acme-corp";
            UUID tenantId = UUID.randomUUID();
            TenantInternalResponse internal = mock(TenantInternalResponse.class);
            when(internal.id()).thenReturn(tenantId);

            Map<String, Object> theme = Map.of("primaryColor", "#FF5733");
            TenantResponse full = new TenantResponse(tenantId, "Acme Corp", "30-12345678-9",
                    Map.of("theme", theme), true, slug);

            when(tenantInternalApi.getTenantBySlug(slug)).thenReturn(internal);
            when(tenantInternalApi.getTenantById(tenantId)).thenReturn(full);

            mockMvc.perform(get("/v1/public/tenant/{slug}", slug)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, s-maxage=604800000, stale-while-revalidate=605800000"))
                    .andExpect(jsonPath("$.name").value("Acme Corp"))
                    .andExpect(jsonPath("$.theme.primaryColor").value("#FF5733"));
        }

        @Test
        void themeMissing_returnsEmptyThemeMap() throws Exception {
            String slug = "acme-corp";
            UUID tenantId = UUID.randomUUID();
            TenantInternalResponse internal = mock(TenantInternalResponse.class);
            when(internal.id()).thenReturn(tenantId);

            TenantResponse full = new TenantResponse(tenantId, "Acme Corp", "30-12345678-9",
                    Map.of(), true, slug);

            when(tenantInternalApi.getTenantBySlug(slug)).thenReturn(internal);
            when(tenantInternalApi.getTenantById(tenantId)).thenReturn(full);

            mockMvc.perform(get("/v1/public/tenant/{slug}", slug)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Acme Corp"))
                    .andExpect(jsonPath("$.theme").isEmpty());
        }
    }

    @Nested
    class Register {

        @Test
        void validRequest_ReturnsUserSetsCookieAndChecksRateLimits() throws Exception {
            UUID userId = UUID.randomUUID();
            when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("1.2.3.4");
            UserResponse userResponse = new UserResponse(userId, null, "Facundo Perez", "facundo@gym.com.ar",
                    "user123", Role.TENANT_ADMIN, true);
            UserLoginInternalResponse loginResponse = new UserLoginInternalResponse(userResponse, null);
            when(registrationInternalApi.register(any(TenantRegisterRequest.class)))
                    .thenReturn(new UserAuthResponse("jwt-token", loginResponse));

            mockMvc.perform(post("/v1/public/tenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tenantFullName": "Facundo Perez",
                                      "email": "facundo@gym.com.ar",
                                      "username": "user123",
                                      "password": "password123",
                                      "tenantName": "Red Hot Chili Arms",
                                      "planId": "%s",
                                      "acceptedTyC": true
                                    }
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.username").value("user123"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("access_token=jwt-token")));

            verify(rateLimiterService, times(2)).checkLimit(any(String.class), anyInt(), anyInt(), any(Duration.class));
        }

        @Test
        void invalidRequest_Returns400() throws Exception {
            mockMvc.perform(post("/v1/public/tenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tenantFullName": "Facundo Perez",
                                      "email": "not-an-email",
                                      "password": "12345678",
                                      "tenantName": "Gym",
                                      "planId": "%s",
                                      "acceptedTyC": false
                                    }
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ValidateEmail {

        @Test
        void validToken_Returns200AndVerifies() throws Exception {
            when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("1.2.3.4");

            mockMvc.perform(post("/v1/public/tenant/register/validate-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"abcdefghijklmnopqrstuvwxyz123456\"}"))
                    .andExpect(status().isOk());

            verify(registrationInternalApi).validateEmail("abcdefghijklmnopqrstuvwxyz123456");
        }

        @Test
        void blankToken_Returns400() throws Exception {
            mockMvc.perform(post("/v1/public/tenant/register/validate-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}

