package com.faust0z.B2BSystem.publics;

import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.access.AccessLogInternalApi;
import com.faust0z.B2BSystem.access.web.AccessLogOccupancyResponse;
import com.faust0z.B2BSystem.catalog.CatalogTemplateInternalApi;
import com.faust0z.B2BSystem.catalog.CatalogTemplateResponse;
import com.faust0z.B2BSystem.catalog.CatalogTemplateSummaryResponse;
import com.faust0z.B2BSystem.contacts.domain.ContactAccessInternalResponse;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/public/catalog")
@Tag(name = "Public Catalog", description = "Public catalog endpoints (no auth required)")
public class CatalogPublicController {

    private final ContactInternalApi contactInternalApi;
    private final TenantInternalApi tenantInternalApi;
    private final CatalogTemplateInternalApi catalogTemplateInternalApi;
    private final AccessLogInternalApi accessLogInternalApi;
    private final RateLimiterService rateLimiterService;

    @Operation(summary = "Public endpoint to check if a contact is active and has access")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
    })
    @GetMapping("/{slug}/contacts/{dni}")
    ContactAccessPublicResponse contactExistsAndIsActive(@PathVariable String slug, @PathVariable String dni, HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        rateLimiterService.checkLimit("pub:contact:ip:" + clientIp, 150, 20, Duration.ofMinutes(5));
        rateLimiterService.checkLimit("pub:contact:dni:" + dni, 5, 1, Duration.ofMinutes(1));

        UUID tenantId = tenantInternalApi.getTenantBySlug(slug).id();
        TenantContext.setTenantId(tenantId);
        try {
            ContactAccessInternalResponse contact = contactInternalApi.findActiveContactByDni(dni);
            log.info("Public checked if contact has access | dni={} | tenantId={}", dni, tenantId);
            return new ContactAccessPublicResponse(contact.firstName() + " " + contact.lastName(), dni,
                    contact.accessUntil() != null && contact.accessUntil().isAfter(Instant.now()));
        } finally {
            TenantContext.clear();
        }
    }

    @Operation(summary = "Public endpoint to get the list of catalog templates")
    @ApiResponse(responseCode = "200", description = "Templates found successfully")
    @GetMapping("/{slug}/templates")
    List<CatalogTemplateSummaryResponse> getTemplateCatalog(@PathVariable String slug, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, s-maxage=900, stale-while-revalidate=86400");

        UUID tenantId = tenantInternalApi.getTenantBySlug(slug).id();
        TenantContext.setTenantId(tenantId);
        try {
            log.info("Public got templates | tenantId={}", tenantId);
            return catalogTemplateInternalApi.findActiveSummaries();
        } finally {
            TenantContext.clear();
        }
    }

    @Operation(summary = "Public endpoint to get a catalog template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template found successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{slug}/templates/{templateId}")
    CatalogTemplateResponse getTemplate(@PathVariable UUID templateId, @PathVariable String slug, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, s-maxage=900, stale-while-revalidate=86400");

        UUID tenantId = tenantInternalApi.getTenantBySlug(slug).id();
        TenantContext.setTenantId(tenantId);
        try {
            log.info("Public got template | templateId={} | tenantId={}", templateId, tenantId);
            return catalogTemplateInternalApi.findById(templateId);
        } finally {
            TenantContext.clear();
        }
    }

    @Operation(summary = "Get estimated current occupancy based on recent access logs")
    @ApiResponse(responseCode = "200", description = "Current occupancy calculated successfully")
    @GetMapping("/{slug}/current-occupancy")
    AccessLogOccupancyResponse getCurrentOccupancy(@RequestParam(name = "minutes", defaultValue = "90") int minutes,
                                                  @PathVariable String slug, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, s-maxage=300, stale-while-revalidate=3600");

        UUID tenantId = tenantInternalApi.getTenantBySlug(slug).id();
        TenantContext.setTenantId(tenantId);
        try {
            log.info("Public got current occupancy | tenantId={}", tenantId);
            return accessLogInternalApi.getCurrentOccupancy(minutes);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return httpRequest.getRemoteAddr();
        }
        return xForwardedFor.split(",")[0].trim();
    }
}
