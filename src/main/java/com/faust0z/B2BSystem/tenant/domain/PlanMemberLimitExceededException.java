package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.tenant.domain.PlanMemberLimitExceededException;
public class PlanMemberLimitExceededException extends DomainRuleException {
    public PlanMemberLimitExceededException() {
        super("La cantidad actual de miembros supera el límite del plan seleccionado", "No se puede cambiar a un plan inferor", HttpStatus.BAD_REQUEST);
    }
}
