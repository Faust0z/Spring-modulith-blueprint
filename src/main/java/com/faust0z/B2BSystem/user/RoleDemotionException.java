package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.user.RoleDemotionException;
public class RoleDemotionException extends DomainRuleException {
    public RoleDemotionException() {
        super("No es posible asignar un rol menor al actual", "Problemas al actualizar el rol", HttpStatus.FORBIDDEN);
    }
}
