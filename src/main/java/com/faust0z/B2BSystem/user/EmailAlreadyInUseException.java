package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.EmailAlreadyInUseException;
public class EmailAlreadyInUseException extends DomainRuleException {
    public EmailAlreadyInUseException() {
        super("El email ya se encuentra en uso", "Problemas con el email", HttpStatus.CONFLICT);
    }
}
