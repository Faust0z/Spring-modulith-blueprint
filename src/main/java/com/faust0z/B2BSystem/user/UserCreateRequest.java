package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserCreateRequest;
@Schema(description = "Datos del nuevo usuario a crear")
public record UserCreateRequest(

        @Schema(description = "Nombre completo", example = "Facundo Pérez")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre completo debe contener entre 2 y 100 caracteres")
        String fullName,

        @Schema(description = "Correo electrónico", example = "facundo@localgym.com.ar")
        @Email(message = "El correo debe ser una dirección válida")
        @NotBlank(message = "El correo es obligatorio")
        String email,

        @Schema(description = "Nombre de usuario", example = "user123")
        @Size(min = 2, max = 50, message = "El nombre de usuario debe contener entre 2 y 50 caracteres")
        String username,

        @Schema(description = "Contraseña", example = "12345678")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @Schema(description = "Rol del usuario", example = "TENANT_USER")
        @NotNull(message = "El rol es obligatorio")
        Role role,

        @Schema(description = "PIN de administración inicial (opcional, se guarda hasheado)", example = "123456")
        String adminPin
) {
}
