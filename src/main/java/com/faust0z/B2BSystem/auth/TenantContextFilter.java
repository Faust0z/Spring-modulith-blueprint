package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import com.faust0z.B2BSystem.auth.TenantContextFilter;
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    public void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = request.getHeader(CORRELATION_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
            MDC.put("userId", jwt.getSubject());

            String tenantIdString = jwt.getClaimAsString("tenantId");
            if (tenantIdString == null && "ADMIN".equals(jwt.getClaimAsString("role"))) {
                tenantIdString = request.getHeader(TENANT_ID_HEADER);
            }

            if (tenantIdString != null) {
                MDC.put("tenantId", tenantIdString);
                try {
                    UUID tenantUuid = UUID.fromString(tenantIdString);
                    TenantContext.setTenantId(tenantUuid);
                    log.debug("TenantContext established for tenant: {}", tenantUuid);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid format for Tenant ID received: {}", tenantIdString);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
            TenantContext.clear();
        }
    }
}