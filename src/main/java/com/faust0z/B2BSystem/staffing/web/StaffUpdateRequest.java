package com.faust0z.B2BSystem.staffing.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Staff update request")
public record StaffUpdateRequest(
        @Schema(description = "First name", example = "Facundo")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Schema(description = "Last name", example = "Pérez")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Schema(description = "DNI", example = "22097162")
        @Pattern(regexp = "^\\d{7,9}$", message = "DNI must contain 7-9 digits")
        String dni
) {
}
