package com.faust0z.B2BSystem.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.user.UserUpdateRequest;
@Schema(description = "Actualización de usuario")
public record UserUpdateRequest(

        @Schema(description = "Nombre completo", example = "John Turner")
        @Size(min = 2, max = 100, message = "El nombre completo debe contener entre 2 y 100 caracteres")
        String fullName,

        @Schema(description = "Correo del usuario", example = "John.doe@example.com")
        @Email(message = "El correo debe ser una dirección válida")
        String email,

        @Schema(description = "Nombre de usuario", example = "user123")
        @Size(min = 2, max = 50, message = "El nombre de usuario debe contener entre 2 y 50 caracteres")
        String username
) {
}
