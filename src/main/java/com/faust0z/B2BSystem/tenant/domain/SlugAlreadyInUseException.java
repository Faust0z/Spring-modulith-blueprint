package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.SlugAlreadyInUseException;
public class SlugAlreadyInUseException extends DomainRuleException {
    public SlugAlreadyInUseException() {
        super("El CUIT ya se encuentra en uso", "Problemas con el CUIT", HttpStatus.CONFLICT);
    }
}