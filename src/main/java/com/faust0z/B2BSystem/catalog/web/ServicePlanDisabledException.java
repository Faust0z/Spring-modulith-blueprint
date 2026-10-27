package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class ServicePlanDisabledException extends DomainRuleException {
    public ServicePlanDisabledException() {
        super("Service plan is disabled", "Service plan disabled", HttpStatus.BAD_REQUEST);
    }
}
