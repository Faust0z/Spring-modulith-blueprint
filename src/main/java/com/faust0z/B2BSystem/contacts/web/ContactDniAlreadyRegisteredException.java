package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class ContactDniAlreadyRegisteredException extends DomainRuleException {
    public ContactDniAlreadyRegisteredException() {
        super("DNI already registered", "DNI already registered", HttpStatus.CONFLICT);
    }
}
