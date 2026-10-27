package com.faust0z.B2BSystem.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

import com.faust0z.B2BSystem.tenant.TenantUpdateThemeRequest;
@Schema(description = "Datos para actualizar las preferencias visuales del negocio")
public record TenantUpdateThemeRequest(
        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "El color de fondo debe ser un hexadecimal válido (ej: #FFFFFF)")
        String bg,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "El color de acento debe ser un hexadecimal válido (ej: #000000)")
        String accent,

        @Schema(description = "Esquema de color predefinido (dark/light)")
        @Pattern(regexp = "^(dark|light)$", message = "El esquema debe ser dark o light")
        String schema,

        @Schema(description = "Paleta de color predefinida")
        @Pattern(regexp = "^(forest|indigo|champagne|terracotta|silver|sage)$", message = "La paleta debe ser forest, indigo, champagne, terracotta, silver o sage")
        String palette
) {
    @AssertTrue(message = "Debe proporcionar bg y accent, o schema y palette (no combinados)")
    public boolean isValidTheme() {
        boolean hasCustom = bg != null && accent != null;
        boolean hasNamed = schema != null && palette != null;
        if (hasCustom == hasNamed) return false;
        if (hasCustom) return true;
        return bg == null && accent == null;
    }

    public boolean isCustom() {
        return bg != null && accent != null;
    }
}
