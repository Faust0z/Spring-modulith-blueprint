package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.MissingCredentialsException;
public class MissingCredentialsException extends DomainRuleException {
    public MissingCredentialsException() {
        super("El email y contraseña son necesarios para crear un usuario con este rol", "Error en la petición", HttpStatus.BAD_REQUEST);
    }
}
