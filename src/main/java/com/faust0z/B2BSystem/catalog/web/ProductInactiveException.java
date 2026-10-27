package com.faust0z.B2BSystem.catalog.web;

import java.util.UUID;

public class ProductInactiveException extends RuntimeException {
    public ProductInactiveException(UUID productId) {
        super("Product is inactive: " + productId);
    }
}
