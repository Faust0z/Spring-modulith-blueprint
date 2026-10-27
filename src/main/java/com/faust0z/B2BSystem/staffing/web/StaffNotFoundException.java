package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class StaffNotFoundException extends DomainRuleException {
    public StaffNotFoundException() {
        super("Staff not found", "Staff not found", HttpStatus.NOT_FOUND);
    }
}
