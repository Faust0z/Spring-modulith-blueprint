package com.faust0z.B2BSystem.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import com.faust0z.B2BSystem.auth.ClientPortalLoginRequest;
@Schema(description = "Credenciales de acceso al portal del inquilino")
public record ClientPortalLoginRequest(
        @Schema(description = "DNI del cliente")
        @NotBlank(message = "El DNI es obligatorio")
        String dni,

        @Schema(description = "PIN del cliente")
        @NotBlank(message = "El PIN es obligatorio")
        String pin
) {
}
