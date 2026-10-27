package com.faust0z.B2BSystem.access.domain;

public class DniNotFoundException extends RuntimeException {
    public DniNotFoundException() {
        super("Identifier not found in staff or contacts");
    }
}
