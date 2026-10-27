package com.faust0z.B2BSystem.tenant.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
@Schema(description = "Solicitud para actualizar un plan de suscripción")
public record SubscriptionPlanUpdateRequest(
        @Schema(description = "Nivel o categoría del plan", example = "PREMIUM")
        @Size(max = 50, message = "El nivel debe contener hasta 50 caracteres")
        String tier,

        @Schema(description = "Precio del plan en ARS", example = "50000")
        @Positive(message = "El precio base debe ser un valor positivo")
        int price,

        @Schema(description = "Cantidad máxima de miembros incluidos (null = ilimitado)", example = "250")
        @Positive(message = "La cantidad máxima de miembros debe ser un valor positivo")
        Integer maxMembers
) {
}