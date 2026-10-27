package com.faust0z.B2BSystem.access.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessLogVoidRequest(
        @NotBlank(message = "You must provide a reason to void the access log")
        @Size(max = 255, message = "The reason cannot exceed 255 characters")
        String reason
) {
}
