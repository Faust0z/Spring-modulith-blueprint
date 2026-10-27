package com.faust0z.B2BSystem.core;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@pinAuthorizationSecurity.isElevated()")
@ApiResponse(responseCode = "403", description = "Requiere autorización por PIN")
public @interface RequirePinAuthorization {
}