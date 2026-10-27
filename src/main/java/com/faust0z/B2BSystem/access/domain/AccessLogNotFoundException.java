package com.faust0z.B2BSystem.access.domain;

import com.faust0z.B2BSystem.core.DomainRuleException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccessLogNotFoundException extends DomainRuleException {
    public AccessLogNotFoundException() {
        super("Access log not found in the system", "Access log not found", HttpStatus.NOT_FOUND);
    }

    public AccessLogNotFoundException(UUID accessLogId) {
        super("Access log with ID not found: " + accessLogId, "Access log not found", HttpStatus.NOT_FOUND);
    }
}
