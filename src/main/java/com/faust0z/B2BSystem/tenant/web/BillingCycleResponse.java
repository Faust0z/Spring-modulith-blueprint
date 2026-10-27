package com.faust0z.B2BSystem.tenant.web;

import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
@Schema(description = "Ciclo de facturación")
public record BillingCycleResponse(
        @Schema(description = "Identificador del ciclo de facturación", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador del tenant", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID tenantId,

        @Schema(description = "Identificador del plan de suscripción", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID planId,

        @Schema(description = "Identificador del plan programado para el próximo ciclo", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID nextPlanId,

        @Schema(description = "Inicio del período de facturación", example = "2024-01-01T00:00:00Z")
        Instant periodStart,

        @Schema(description = "Fin del período de facturación", example = "2024-01-31T23:59:59Z")
        Instant periodEnd,

        @Schema(description = "Cantidad máxima de miembros registrados en el período", example = "150")
        int peakMembers,

        @Schema(description = "Porcentaje de descuento aplicado al ciclo", example = "10")
        int discountPercentage,

        @Schema(description = "Precio pagado", example = "50000")
        int amount,

        @Schema(description = "Estado actual del ciclo de facturación", example = "ACTIVE")
        CycleStatus status,

        @Schema(description = "Indica si el ciclo fue para proveer la demo", example = "50000")
        boolean isDemo
) {
}
