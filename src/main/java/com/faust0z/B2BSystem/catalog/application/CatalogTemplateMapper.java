package com.faust0z.B2BSystem.catalog.application;

import com.faust0z.B2BSystem.catalog.domain.CatalogTemplateEntity;
import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateCreateRequest;
import com.faust0z.B2BSystem.catalog.CatalogTemplateResponse;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface CatalogTemplateMapper {

    @Mapping(target = "deletedAt", source = "deletedAt")
    CatalogTemplateResponse toResponse(CatalogTemplateEntity entity);

    CatalogTemplateEntity toEntity(CatalogTemplateCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRoutineFromRequest(CatalogTemplateUpdateRequest request, @MappingTarget CatalogTemplateEntity entity);
}
