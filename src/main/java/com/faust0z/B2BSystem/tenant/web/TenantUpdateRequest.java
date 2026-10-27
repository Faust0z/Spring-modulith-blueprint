package com.faust0z.B2BSystem.tenant.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
@Schema(description = "Actualización de datos del Negocio (Tenant)")
public record TenantUpdateRequest(
        @Schema(description = "Nombre del negocio", example = "Acme Corp")
        @Size(min = 2, max = 100, message = "El nombre debe contener entre 2 y 100 caracteres")
        String name,

        @Schema(description = "CUIT del negocio", example = "30-12345678-9")
        @Pattern(regexp = "^\\d{2}-\\d{8}-\\d$", message = "CUIT debe seguir el formato XX-YYYYYYYY-Z")
        String taxId,

        @Schema(description = "Las configuraciones del sitio del negocio", example = "{\"theme\": \"dark\", \"allowLateCancellations\": true}")
        Map<String, Object> settings,

        @Schema(description = "Slug para url", example = "templo-fitness")
        @Pattern(regexp = "^[a-z_-]+$", message = "El slug debe estar en minúsculas y sus palabras separadas por guiones (-)")
        @Size(min = 1, max = 30, message = "El slug debe contener entre 1 y 30 caracteres")
        String slug
) {
}
