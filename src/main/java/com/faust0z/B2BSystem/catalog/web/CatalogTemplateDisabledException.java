package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class CatalogTemplateDisabledException extends DomainRuleException {
    public CatalogTemplateDisabledException() {
        super("La rutina se encuentra deshabilitada", "Problemas con la rutina", HttpStatus.BAD_REQUEST);
    }
}
