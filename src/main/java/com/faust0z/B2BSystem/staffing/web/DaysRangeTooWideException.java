package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class DaysRangeTooWideException extends DomainRuleException {
    public DaysRangeTooWideException(int days) {
        super("Date range must not exceed " + days + " days", "Date range too wide", HttpStatus.BAD_REQUEST);
    }
}
