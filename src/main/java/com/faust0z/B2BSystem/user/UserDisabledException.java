package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.UserDisabledException;
public class UserDisabledException extends DomainRuleException {
    public UserDisabledException() {
        super("El usuario se encuentra deshabilitado", "Problema con el usuario", HttpStatus.FORBIDDEN);
    }
}