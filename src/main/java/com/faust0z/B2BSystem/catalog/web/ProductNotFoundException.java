package com.faust0z.B2BSystem.catalog.web;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super("Product not found: " + productId);
    }
}
