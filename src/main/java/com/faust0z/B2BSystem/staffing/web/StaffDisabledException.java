package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class StaffDisabledException extends DomainRuleException {
    public StaffDisabledException() {
        super("Staff is disabled", "Staff disabled", HttpStatus.FORBIDDEN);
    }
}
