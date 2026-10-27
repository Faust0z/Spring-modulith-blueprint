package com.faust0z.B2BSystem.core;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainRuleException {
    public ResourceNotFoundException() {
        super("No se pudo encontrar el recurso solicitado", "Recurso no encontrado", HttpStatus.NOT_FOUND);
    }
}