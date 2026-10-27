package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.RoleEscalationException;
public class RoleEscalationException extends DomainRuleException {
    public RoleEscalationException() {
        super("No tiene permisos para asignar ese rol", "Problemas al actualizar el rol", HttpStatus.FORBIDDEN);
    }
}
