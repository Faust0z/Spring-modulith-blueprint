package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.TenantLoginDisabledException;
public class TenantLoginDisabledException extends DomainRuleException {
    public TenantLoginDisabledException() {
        super("El establecimiento se encuentra temporalmente deshabilitado. Por favor, contacte a la administración.",
                "Fallo al iniciar sesión", HttpStatus.FORBIDDEN);
    }
}