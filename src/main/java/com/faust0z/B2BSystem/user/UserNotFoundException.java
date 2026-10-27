package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.UserNotFoundException;
public class UserNotFoundException extends DomainRuleException {
    public UserNotFoundException() {
        super("No se encontró el usuario en el sistema", "Usuario no encontrado", HttpStatus.NOT_FOUND);
    }
}