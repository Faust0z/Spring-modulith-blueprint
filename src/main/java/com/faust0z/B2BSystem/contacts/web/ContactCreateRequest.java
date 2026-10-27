package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.contacts.domain.GenderCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "New contact request")
public record ContactCreateRequest(
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
        String dni,

        @Schema(description = "Email", example = "email@email.com")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Birth date", example = "1995-05-20")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @Schema(description = "Phone number", example = "+543329771432")
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid phone format")
        String phoneNumber,

        @Schema(description = "Gender", example = "MASCULINO")
        GenderCategory gender
) {
}
