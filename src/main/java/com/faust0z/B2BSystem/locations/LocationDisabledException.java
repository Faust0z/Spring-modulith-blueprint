package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.locations.LocationDisabledException;
public class LocationDisabledException extends DomainRuleException {
    public LocationDisabledException() {
        super("La sede se encuentra deshabilitada", "Problemas con la sede", HttpStatus.BAD_REQUEST);
    }
}