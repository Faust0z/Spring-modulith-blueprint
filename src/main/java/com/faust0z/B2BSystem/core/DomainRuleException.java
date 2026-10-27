package com.faust0z.B2BSystem.core;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainRuleException extends RuntimeException {
    private final String title;
    private final HttpStatus status;

    public DomainRuleException(String message, String title, HttpStatus status) {
        super(message);
        this.status = status;
        this.title = title;
    }
}