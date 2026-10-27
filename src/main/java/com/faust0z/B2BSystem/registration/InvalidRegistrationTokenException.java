package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.registration.InvalidRegistrationTokenException;
public class InvalidRegistrationTokenException extends DomainRuleException {
    public InvalidRegistrationTokenException() {
        super("El código proporcionado es inválido o ya fue utilizado", "Problemas para completar el proceso", HttpStatus.UNAUTHORIZED);
    }
}
