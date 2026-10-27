package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.CycleAlreadyPaidException;
public class CycleAlreadyPaidException extends DomainRuleException {
    public CycleAlreadyPaidException() {
        super("El ciclo de facturación ya está pagado", "Ciclo ya pagado", HttpStatus.CONFLICT);
    }
}
