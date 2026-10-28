package com.faust0z.B2BSystem.catalog.application;

import com.faust0z.B2BSystem.catalog.domain.ServicePlanEntity;
import com.faust0z.B2BSystem.catalog.web.ServicePlanCreateRequest;
import com.faust0z.B2BSystem.catalog.ServicePlanResponse;
import com.faust0z.B2BSystem.catalog.web.ServicePlanUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ServicePlanMapper {

    @Mapping(target = "deletedAt", source = "deletedAt")
    ServicePlanResponse toResponse(ServicePlanEntity servicePlan);

    ServicePlanEntity toEntity(ServicePlanCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateServicePlanFromRequest(ServicePlanUpdateRequest request, @MappingTarget ServicePlanEntity entity);
}
