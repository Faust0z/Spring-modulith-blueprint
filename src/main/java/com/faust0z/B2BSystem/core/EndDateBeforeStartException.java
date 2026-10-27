package com.faust0z.B2BSystem.core;

import org.springframework.http.HttpStatus;

public class EndDateBeforeStartException extends DomainRuleException {
    public EndDateBeforeStartException() {
        super("La fecha de fin debe ser posterior a la fecha de inicio", "Rango de fechas inválido", HttpStatus.BAD_REQUEST);
    }
}
