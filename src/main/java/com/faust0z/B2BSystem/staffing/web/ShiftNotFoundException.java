package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class ShiftNotFoundException extends DomainRuleException {
    public ShiftNotFoundException() {
        super("Shift not found", "Shift not found", HttpStatus.NOT_FOUND);
    }
}
