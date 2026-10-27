package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.locations.MissingBranchHeaderException;
public class MissingBranchHeaderException extends DomainRuleException {
    public MissingBranchHeaderException() {
        super("Es necesario enviar el id de la sede actual en el header X-Branch-Id para completar esta request", "Error en la petición", HttpStatus.BAD_REQUEST);
    }
}
