package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class ContactDisabledException extends DomainRuleException {
    public ContactDisabledException() {
        super("Contact is disabled", "Contact disabled", HttpStatus.BAD_REQUEST);
    }
}
