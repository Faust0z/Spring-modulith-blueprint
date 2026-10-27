package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class IncorrectMinutesAmountException extends DomainRuleException {
    public IncorrectMinutesAmountException() {
        super("Minutes must be greater than 0 and less than 1440", "Invalid minutes amount", HttpStatus.BAD_REQUEST);
    }
}
