package com.faust0z.B2BSystem.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.registration.EmailVerificationRequest;
@Schema(description = "Token de verificación de correo recibido en el magic link")
public record EmailVerificationRequest(
        @Schema(description = "Token contenido en el magic link", example = "aZ9x...")
        @NotBlank(message = "El token es obligatorio")
        @Size(min = 16, max = 128, message = "El token debe tener un formato válido")
        String token
) {
}