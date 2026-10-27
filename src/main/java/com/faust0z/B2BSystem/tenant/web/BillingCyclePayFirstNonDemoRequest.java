package com.faust0z.B2BSystem.tenant.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.web.BillingCyclePayFirstNonDemoRequest;
@Schema(description = "Solicitud para registrar el primer pago de un tenant (sin ciclos previos o con demo activa)")
public record BillingCyclePayFirstNonDemoRequest(
        @Schema(description = "Identificador del tenant", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El tenant es obligatorio")
        UUID tenantId,

        @Schema(description = "Identificador del plan de suscripción", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El plan es obligatorio")
        UUID planId,

        @Schema(description = "Monto opcional para sobrescribir el precio base del plan", example = "50000")
        @PositiveOrZero(message = "El monto debe ser positivo o cero")
        Integer amount,

        @Schema(description = "Porcentaje de descuento opcional", example = "15")
        @Positive(message = "El descuento debe ser positivo")
        Integer discountPercentage,

        @Schema(description = "Meses restantes de descuento opcional", example = "3")
        @PositiveOrZero(message = "Los meses restantes de descuento deben ser positivos o cero")
        Integer remainingDiscountMonths
) {
}
