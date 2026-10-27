package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.auth.InvalidTokenException;
public class InvalidTokenException extends DomainRuleException {
    public InvalidTokenException() {
        super("Su token no le permite acceso a este sitio", "Problemas con el token", HttpStatus.BAD_REQUEST);
    }
}
