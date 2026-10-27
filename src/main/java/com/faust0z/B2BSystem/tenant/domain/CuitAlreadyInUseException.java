package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.CuitAlreadyInUseException;
public class CuitAlreadyInUseException extends DomainRuleException {
    public CuitAlreadyInUseException() {
        super("El CUIT ya se encuentra en uso", "Problemas con el CUIT", HttpStatus.CONFLICT);
    }
}