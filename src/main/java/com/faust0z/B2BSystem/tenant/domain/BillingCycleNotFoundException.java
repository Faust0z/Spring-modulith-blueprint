package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.BillingCycleNotFoundException;
public class BillingCycleNotFoundException extends DomainRuleException {
    public BillingCycleNotFoundException() {
        super("No se encontró el ciclo de facturación", "Ciclo de facturación no encontrado", HttpStatus.NOT_FOUND);
    }
}