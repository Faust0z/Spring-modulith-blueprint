package com.faust0z.B2BSystem.core;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends DomainRuleException {
    public TooManyRequestsException(long duration, String unit) {
        super("Demasiados intentos, espere " + duration + " " + unit + " e intente de nuevo", "Demasiadas peticiones", HttpStatus.TOO_MANY_REQUESTS);
    }
}