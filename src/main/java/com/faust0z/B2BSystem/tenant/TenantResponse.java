package com.faust0z.B2BSystem.tenant;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
@Schema(description = "Representación de Negocio (Tenant)")
public record TenantResponse(
        @Schema(description = "ID del negocio", example = "e428d134-616f-41ae-b060-4284319a74ed")
        UUID id,

        @Schema(description = "Nombre del negocio", example = "Acme Corp")
        String name,

        @Schema(description = "CUIT del negocio", example = "30-12345678-9")
        String taxId,

        @Schema(description = "Las configuraciones del sitio del negocio", example = "{\"theme\": \"dark\", \"allowLateCancellations\": true}")
        Map<String, Object> settings,

        @Schema(description = "Estado del negocio", example = "true")
        boolean isActive,

        @Schema(description = "Slug del negocio para url", example = "templo-fitness")
        String slug
) {
}
