package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

public class ServicePlanNotFoundException extends DomainRuleException {
    public ServicePlanNotFoundException() {
        super("Service plan not found", "Service plan not found", HttpStatus.NOT_FOUND);
    }
}
