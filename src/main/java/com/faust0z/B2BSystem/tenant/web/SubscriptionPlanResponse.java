package com.faust0z.B2BSystem.tenant.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
@Schema(description = "Plan de suscripción")
public record SubscriptionPlanResponse(
        @Schema(description = "Identificador del plan de suscripción", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nivel o categoría del plan", example = "PREMIUM")
        String tier,

        @Schema(description = "Precio base del plan en ARS", example = "50000")
        int price,

        @Schema(description = "Cantidad máxima de miembros incluidos (null = ilimitado)", example = "250")
        Integer maxMembers
) {
}