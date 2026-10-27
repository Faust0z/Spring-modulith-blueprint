package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanMapper;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionPlanMapper {
    public SubscriptionPlanResponse toResponse(SubscriptionPlanEntity entity);

    public SubscriptionPlanEntity toEntity(SubscriptionPlanCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(SubscriptionPlanUpdateRequest request, @MappingTarget SubscriptionPlanEntity entity);
}