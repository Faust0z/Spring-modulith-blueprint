package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.UsernameAlreadyInUseException;
public class UsernameAlreadyInUseException extends DomainRuleException {
    public UsernameAlreadyInUseException() {
        super("El nombre de usuario ya se encuentra en uso", "Problemas con el nombre de usuario", HttpStatus.CONFLICT);
    }
}
