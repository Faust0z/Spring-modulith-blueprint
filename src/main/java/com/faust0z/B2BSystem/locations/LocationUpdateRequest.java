package com.faust0z.B2BSystem.locations;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.locations.LocationUpdateRequest;
public record LocationUpdateRequest(
        @Schema(description = "Nombre de la sede", example = "Maipú 774")
        @Size(min = 2, max = 100, message = "El nombre debe contener entre 2 y 100 caracteres")
        String name
) {
}
