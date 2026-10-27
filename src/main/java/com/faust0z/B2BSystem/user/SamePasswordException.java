package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.SamePasswordException;
public class SamePasswordException extends DomainRuleException {
    public SamePasswordException() {
        super("La contraseña debe ser diferente de la anterior", "Problema con la contraseña", HttpStatus.BAD_REQUEST);
    }
}
