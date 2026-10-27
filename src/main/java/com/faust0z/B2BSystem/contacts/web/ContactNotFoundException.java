package com.faust0z.B2BSystem.contacts.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class ContactNotFoundException extends DomainRuleException {
    public ContactNotFoundException() {
        super("Contact not found", "Contact not found", HttpStatus.NOT_FOUND);
    }
}
