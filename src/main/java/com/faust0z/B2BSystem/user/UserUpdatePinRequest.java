package com.faust0z.B2BSystem.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.faust0z.B2BSystem.user.UserUpdatePinRequest;
public record UserUpdatePinRequest(
        @Schema(description = "El nuevo adminPin del usuario", example = "123456")
        @NotNull(message = "El adminPin es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El adminPin debe contener 6 números")
        String adminPin
) {
}
