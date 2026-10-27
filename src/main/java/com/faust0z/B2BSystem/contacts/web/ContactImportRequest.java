package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.contacts.domain.GenderCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ContactImportRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String nombre,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String apellido,

        @NotBlank(message = "DNI is required")
        @Pattern(regexp = "^\\d{7,9}$", message = "DNI must contain 7-9 digits")
        String dni,

        @Schema(description = "Email", example = "email@email.com")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Access expiration date", example = "16/05/2026")
        LocalDate accessUntil,

        @Schema(description = "Birth date", example = "16/05/2000")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @Schema(description = "Phone number", example = "+543329771432")
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid phone format")
        String phoneNumber,

        @Schema(description = "Gender", example = "MASCULINO")
        GenderCategory gender
) {
}
