package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.PlanChangeAlreadyScheduledException;
public class PlanChangeAlreadyScheduledException extends DomainRuleException {
    public PlanChangeAlreadyScheduledException() {
        super("Ya hay un cambio de plan programado para el próximo ciclo", "Imposible cambiar el plan", HttpStatus.CONFLICT);
    }
}
