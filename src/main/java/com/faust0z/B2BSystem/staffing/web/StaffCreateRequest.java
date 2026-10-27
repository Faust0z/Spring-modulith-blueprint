package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.staffing.domain.StaffIdentifierType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New staff request")
public record StaffCreateRequest(
        @Schema(description = "First name", example = "Facundo")
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Schema(description = "Last name", example = "Pérez")
        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Schema(description = "DNI", example = "22097162")
        @NotBlank(message = "DNI is required")
        @Pattern(regexp = "^\\d{7,9}$", message = "DNI must contain 7-9 digits")
        String dni
) {
}
