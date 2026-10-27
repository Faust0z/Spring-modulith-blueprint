package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.TenantDisabledException;
public class TenantDisabledException extends DomainRuleException {
    public TenantDisabledException() {
        super("El establecimiento se encuentra deshabilitado", "Problema con el establecimiento", HttpStatus.FORBIDDEN);
    }
}