package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.TenantPeriodExpiredException;
public class TenantPeriodExpiredException extends DomainRuleException {
    public TenantPeriodExpiredException() {
        super("El establecimiento se encuentra deshabilitado por falta de pago. Por favor, contacte a la administración.",
                "Fallo al iniciar sesión", HttpStatus.PAYMENT_REQUIRED);
    }
}