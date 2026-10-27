package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class CatalogTemplateNotFoundException extends DomainRuleException {
    public CatalogTemplateNotFoundException() {
        super("No se encontró la rutina", "Rutina no encontrada", HttpStatus.NOT_FOUND);
    }
}
