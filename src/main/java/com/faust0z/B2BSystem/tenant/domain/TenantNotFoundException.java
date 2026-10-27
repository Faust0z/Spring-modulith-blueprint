package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.TenantNotFoundException;
public class TenantNotFoundException extends DomainRuleException {
    public TenantNotFoundException() {
        super("Negocio no encontrado en el sistema", "Negocio no encontrado", HttpStatus.NOT_FOUND);
    }
}