package com.faust0z.B2BSystem.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import com.faust0z.B2BSystem.registration.TenantRegisterRequest;
@Schema(description = "Datos para registrar un nuevo negocio en un solo paso")
public record TenantRegisterRequest(
        @Schema(description = "Nombre completo del dueño", example = "Facundo Pérez")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre completo debe contener entre 2 y 100 caracteres")
        String tenantFullName,

        @Schema(description = "Correo electrónico del dueño", example = "facundo@localgym.com.ar")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El correo debe ser una dirección válida")
        String email,

        @Schema(description = "Nombre de usuario (opcional)", example = "user123")
        @Size(min = 2, max = 50, message = "El nombre de usuario debe contener entre 2 y 50 caracteres")
        String username,

        @Schema(description = "Contraseña", example = "12345678")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @Schema(description = "Nombre del negocio", example = "Red Hot Chili Arms")
        @NotBlank(message = "El nombre del negocio es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre del negocio debe contener entre 2 y 50 caracteres")
        String tenantName,

        @Schema(description = "Plan de suscripción del negocio", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El plan es obligatorio")
        UUID planId,

        @Schema(description = "Aceptación de los Términos y Condiciones", example = "true")
        @AssertTrue(message = "Debe aceptar los Términos y Condiciones.")
        boolean acceptedTyC
) {
}