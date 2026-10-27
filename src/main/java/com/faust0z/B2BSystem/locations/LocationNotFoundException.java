package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.locations.LocationNotFoundException;
public class LocationNotFoundException extends DomainRuleException {
    public LocationNotFoundException() {
        super("No se encontró la sede", "Sede no encontrada", HttpStatus.NOT_FOUND);
    }
}