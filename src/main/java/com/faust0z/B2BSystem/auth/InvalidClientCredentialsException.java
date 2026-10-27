package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.auth.InvalidClientCredentialsException;
public class InvalidClientCredentialsException extends DomainRuleException {
    public InvalidClientCredentialsException() {
        super("Dni o token incorrecto", "Problemas para iniciar sesión", HttpStatus.UNAUTHORIZED);
    }
}
