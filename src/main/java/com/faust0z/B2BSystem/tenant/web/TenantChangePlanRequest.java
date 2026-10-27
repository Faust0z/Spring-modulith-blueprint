package com.faust0z.B2BSystem.tenant.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.web.TenantChangePlanRequest;
@Schema(description = "Solicitud para cambiar de plan de suscripción (bajada de plan voluntaria)")
public record TenantChangePlanRequest(
        @Schema(description = "Identificador del plan al que se quiere cambiar", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El plan es obligatorio")
        UUID planId
) {
}
