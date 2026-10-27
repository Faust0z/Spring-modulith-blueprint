package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.IncorrectPasswordException;
public class IncorrectPasswordException extends DomainRuleException {
    public IncorrectPasswordException() {
        super("La contraseña actual es incorrecta", "Problema con la contraseña", HttpStatus.BAD_REQUEST);
    }
}
