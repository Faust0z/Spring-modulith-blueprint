package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
@Schema(description = "Respuesta de inicio de sesión")
public record UserLoginInternalResponse(
        @Schema(description = "Detalles del usuario autenticado", implementation = UserResponse.class)
        UserResponse user,

        @Schema(description = "Detalles del negocio y configuraciones", implementation = TenantResponse.class)
        TenantResponse tenant
) {
}
