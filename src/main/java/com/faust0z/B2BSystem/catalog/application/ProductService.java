package com.faust0z.B2BSystem.catalog.application;

import com.faust0z.B2BSystem.catalog.ProductInternalApi;
import com.faust0z.B2BSystem.catalog.ProductResponse;
import com.faust0z.B2BSystem.catalog.domain.ProductEntity;
import com.faust0z.B2BSystem.catalog.infrastructure.ProductRepository;
import com.faust0z.B2BSystem.catalog.web.ProductCreateRequest;
import com.faust0z.B2BSystem.catalog.web.ProductNotFoundException;
import com.faust0z.B2BSystem.catalog.web.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements ProductInternalApi {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Cacheable(value = "tenant_products", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.debug("Querying products");
        return productRepository.findBy(
                (root, query, cb) -> cb.conjunction(),
                fluent -> fluent
                        .as(ProductResponse.class)
                        .sortBy(Sort.by(Sort.Direction.ASC, "price"))
                        .all()
        );
    }

    @CacheEvict(value = "tenant_products", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.debug("Creating product");

        ProductEntity productEntity = productMapper.toEntity(request);
        productEntity = productRepository.save(productEntity);

        log.debug("Product created | productId={}", productEntity.getId());
        return productMapper.toResponse(productEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenant_products", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "product", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #productId")
    })
    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductUpdateRequest request) {
        log.debug("Updating product | productId={}", productId);

        ProductEntity productEntity = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        productMapper.updateProductFromRequest(request, productEntity);

        log.debug("Successfully updated product | productId={}", productId);
        return productMapper.toResponse(productEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenant_products", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "product", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #productId")
    })
    @Transactional
    public ProductResponse toggleProductStatus(UUID productId) {
        log.debug("Updating product status | productId={}", productId);

        ProductEntity productEntity = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));

        if (productEntity.getDeletedAt() == null) {
            productEntity.softDelete();
        } else {
            productEntity.setDeletedAt(null);
        }

        log.debug("Product status updated | productId={} | deletedAt={}", productId, productEntity.getDeletedAt());
        return productMapper.toResponse(productEntity);
    }

    @Override
    public Optional<ProductResponse> findById(UUID id) {
        return productRepository.findById(id).map(productMapper::toResponse);
    }
}
