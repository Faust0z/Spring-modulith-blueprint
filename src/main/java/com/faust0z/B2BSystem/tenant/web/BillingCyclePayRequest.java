package com.faust0z.B2BSystem.tenant.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.web.BillingCyclePayRequest;
@Schema(description = "Solicitud para liquidar el pago de un ciclo de facturación existente")
public record BillingCyclePayRequest(
        @Schema(description = "Identificador del tenant", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El tenant es obligatorio")
        UUID tenantId,

        @Schema(description = "Identificador del ciclo de facturación a pagar", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El ciclo de facturación es obligatorio")
        UUID cycleId,

        @Schema(description = "Monto extra opcional para sobrecargos", example = "5000")
        Integer optionalExtra
) {
}
