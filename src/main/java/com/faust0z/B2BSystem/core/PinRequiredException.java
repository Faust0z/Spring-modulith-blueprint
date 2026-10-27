package com.faust0z.B2BSystem.core;

import org.springframework.http.HttpStatus;

public class PinRequiredException extends DomainRuleException {
    public PinRequiredException() {
        super("Se requiere elevación de privilegios via pin", "Problemas para completar la solicitud", HttpStatus.FORBIDDEN);
    }
}