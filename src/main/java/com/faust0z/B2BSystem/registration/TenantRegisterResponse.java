package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import com.faust0z.B2BSystem.registration.TenantRegisterResponse;
@Schema(description = "Respuesta al registrar un negocio en un solo paso")
public record TenantRegisterResponse(
        @Schema(description = "Detalles del nuevo usuario y negocio autenticados")
        UserLoginInternalResponse auth
) {
}