package com.faust0z.B2BSystem.access.domain;

public class AmbiguousIdentityException extends RuntimeException {
    public AmbiguousIdentityException() {
        super("Ambiguous identity: identifier matches both staff and contact");
    }
}
