package com.faust0z.B2BSystem.publics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

import com.faust0z.B2BSystem.publics.TenantPublicResponse;
@Schema(description = "Public tenant info with theme for portal branding")
public record TenantPublicResponse(
        @Schema(description = "Nombre del negocio")
        String name,

        @Schema(description = "Opciones visuales del tema")
        Map<String, Object> theme
) {
}
