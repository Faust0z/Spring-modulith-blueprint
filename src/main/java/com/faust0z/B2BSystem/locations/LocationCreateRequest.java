package com.faust0z.B2BSystem.locations;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.locations.LocationCreateRequest;
public record LocationCreateRequest(
        @Schema(description = "Nombre", example = "Laprida 1287")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe contener entre 2 y 50 caracteres")
        String name
) {
}
