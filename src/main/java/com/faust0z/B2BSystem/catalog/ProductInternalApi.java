package com.faust0z.B2BSystem.catalog;

import com.faust0z.B2BSystem.catalog.domain.ProductEntity;
import com.faust0z.B2BSystem.catalog.ProductResponse;

import java.util.Optional;
import java.util.UUID;

public interface ProductInternalApi {
    Optional<ProductResponse> findById(UUID id);
}
