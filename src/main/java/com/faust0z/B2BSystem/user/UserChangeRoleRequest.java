package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserChangeRoleRequest;
@Schema(description = "Datos para cambiar el rol de un usuario")
public record UserChangeRoleRequest(
        @Schema(description = "El nuevo rol para asignar al usuario", example = "TENANT_ADMIN")
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
