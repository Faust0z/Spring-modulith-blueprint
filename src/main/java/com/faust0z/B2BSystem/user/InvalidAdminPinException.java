package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.InvalidAdminPinException;
public class InvalidAdminPinException extends DomainRuleException {
    public InvalidAdminPinException() {
        super("El PIN es incorrecto", "PIN inválido", HttpStatus.UNAUTHORIZED);
    }
}
