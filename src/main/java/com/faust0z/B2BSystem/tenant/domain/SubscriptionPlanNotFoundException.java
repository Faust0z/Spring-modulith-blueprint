package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
public class SubscriptionPlanNotFoundException extends DomainRuleException {
    public SubscriptionPlanNotFoundException() {
        super("No se encontró el plan de subscripción", "Plan de subscripción no encontrado", HttpStatus.NOT_FOUND);
    }
}