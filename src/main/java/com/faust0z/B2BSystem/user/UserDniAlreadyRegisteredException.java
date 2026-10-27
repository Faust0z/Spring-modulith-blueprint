package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.UserDniAlreadyRegisteredException;
public class UserDniAlreadyRegisteredException extends DomainRuleException {
    public UserDniAlreadyRegisteredException() {
        super("El DNI ya se encuentra registrado para el usuario", "Problemas con el DNI", HttpStatus.CONFLICT);
    }
}
