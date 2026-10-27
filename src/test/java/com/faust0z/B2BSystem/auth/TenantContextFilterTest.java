package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.auth.TenantContextFilter;
class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    void doFilterInternal_WithTenantClaim_SetsContextAndMdc() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock.token").header("alg", "none").subject("user-1")
                .claim("tenantId", tenantId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
            assertThat(MDC.get("tenantId")).isEqualTo(tenantId.toString());
        });

        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(MDC.get("tenantId")).isNull();
    }

    @Test
    void doFilterInternal_AdminWithHeader_SetsContext() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock.token").header("alg", "none").subject("admin-1")
                .claim("role", "ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Tenant-Id")).thenReturn(tenantId.toString());
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilterInternal(request, response, (req, res) -> assertThat(TenantContext.getTenantId()).isEqualTo(tenantId));
    }

    @Test
    void doFilterInternal_InvalidTenantUuid_DoesNotSetContext() throws Exception {
        Jwt jwt = Jwt.withTokenValue("mock.token").header("alg", "none").subject("user-1")
                .claim("tenantId", "invalid-uuid-string").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(TenantContext.getTenantId()).isNull();
            assertThat(MDC.get("tenantId")).isEqualTo("invalid-uuid-string");
        });
    }
}