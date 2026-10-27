package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import com.faust0z.B2BSystem.locations.LocationNameExistsException;
public class LocationNameExistsException extends DomainRuleException {
    public LocationNameExistsException() {
        super("Una sede con este nombre ya se encuentra registrada", "Problemas para crear la sede", HttpStatus.CONFLICT);
    }
}
