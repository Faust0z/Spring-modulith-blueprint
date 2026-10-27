package com.faust0z.B2BSystem.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.auth.UserLoginRequest;
@Schema(description = "Carga útil para iniciar sesión")
public record UserLoginRequest(
        @Schema(description = "Correo electrónico o nombre de usuario", example = "admin@test.com o admin_test")
        @NotBlank(message = "El identificador (email o usuario) es obligatorio.")
        String identifier,

        @Schema(description = "Contraseña del usuario", example = "12345678")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @NotBlank(message = "La contraseña es obligatoria.")
        String password
) {
}
