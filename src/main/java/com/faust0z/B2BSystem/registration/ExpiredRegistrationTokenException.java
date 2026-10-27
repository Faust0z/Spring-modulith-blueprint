package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.registration.ExpiredRegistrationTokenException;
public class ExpiredRegistrationTokenException extends DomainRuleException {
    public ExpiredRegistrationTokenException() {
        super("El código proporcionado está vencido", "Problemas para completar el proceso", HttpStatus.UNAUTHORIZED);
    }
}
