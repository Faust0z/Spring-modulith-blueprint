package com.faust0z.B2BSystem.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.user.UserUpdatePasswordRequest;
@Schema(description = "Actualización de la contraseña del usuario")
public record UserUpdatePasswordRequest(
        @Schema(description = "Contraseña actual", example = "Pass123")
        String currentPassword,

        @Schema(description = "Nueva contraseña", example = "NewPass123")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String newPassword
) {
}
