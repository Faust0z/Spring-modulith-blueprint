package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.UserLoginDisabledException;
public class UserLoginDisabledException extends DomainRuleException {
    public UserLoginDisabledException() {
        super("El usuario se encuentra deshabilitado", "Fallo al iniciar sesión", HttpStatus.FORBIDDEN);
    }
}