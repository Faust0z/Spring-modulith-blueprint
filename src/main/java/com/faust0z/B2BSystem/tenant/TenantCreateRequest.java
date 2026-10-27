package com.faust0z.B2BSystem.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
@Schema(description = "Datos del nuevo negocio (Tenant) a crear")
public record TenantCreateRequest(
        @Schema(description = "Nombre del negocio", example = "Red Hot Chili Arms")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe contener entre 2 y 50 caracteres")
        String name,

        @Schema(description = "CUIT del negocio en formato estricto", example = "30-12345678-9")
        @Pattern(regexp = "^\\d{2}-\\d{8}-\\d$", message = "CUIT debe seguir el formato XX-YYYYYYYY-Z")
        String taxId,

        @Schema(description = "Slug para url", example = "templo-fitness")
        @Pattern(regexp = "^[a-z0-9_-]+$", message = "El slug debe estar en minúsculas y sus palabras separadas por guiones (-)")
        @Size(min = 1, max = 30, message = "El slug debe contener entre 1 y 30 caracteres")
        String slug,

        @Schema(description = "Plan de suscripción del negocio", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El plan es obligatorio")
        UUID planId
) {
}
