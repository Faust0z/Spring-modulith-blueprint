package com.faust0z.B2BSystem.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserResponse;
@Schema(description = "DTO de usuario")
public record UserResponse(
        @Schema(description = "ID del usuario", example = "1c70cf24-cea0-41a9-a4e3-38628c075a43")
        UUID id,

        @Schema(description = "ID del negocio al que pertenece", example = "1c70cf24-awok-heh1-awe3-38612c075a43")
        UUID tenantId,

        @Schema(description = "Nombre completo del usuario", example = "John Turner")
        String fullName,

        @Schema(description = "Correo del usuario", example = "John.doe@example.com")
        String email,

        @Schema(description = "Nombre de usuario del usuario", example = "user123")
        String username,

        @Schema(description = "Rol del usuario", example = "ADMIN")
        Role role,

        @Schema(description = "Estado de la cuenta del usuario", example = "true")
        Boolean isActive
) {
}
