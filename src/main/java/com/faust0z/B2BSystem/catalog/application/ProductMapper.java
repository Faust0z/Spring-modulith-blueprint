package com.faust0z.B2BSystem.catalog.application;

import com.faust0z.B2BSystem.catalog.domain.ProductEntity;
import com.faust0z.B2BSystem.catalog.web.ProductCreateRequest;
import com.faust0z.B2BSystem.catalog.ProductResponse;
import com.faust0z.B2BSystem.catalog.web.ProductUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ProductMapper {

    @Mapping(target = "deletedAt", source = "deletedAt")
    ProductResponse toResponse(ProductEntity productEntity);

    ProductEntity toEntity(ProductCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductFromRequest(ProductUpdateRequest request, @MappingTarget ProductEntity entity);
}
