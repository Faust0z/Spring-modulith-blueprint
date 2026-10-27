package com.faust0z.B2BSystem.core;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "No autorizado (Falta token o expiró)"),
        @ApiResponse(responseCode = "403", description = "Prohibido (Requiere rol ADMIN o TENANT_ADMIN)")
})
public @interface RequireAdmin {
}