package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.faust0z.B2BSystem.tenant.TenantCreateRequest;
import com.faust0z.B2BSystem.tenant.TenantResponse;
import com.faust0z.B2BSystem.tenant.application.TenantMapper;
import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.web.TenantUpdateRequest;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    public TenantResponse toResponse(Tenant tenant);

    public Tenant toEntity(TenantCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTenantFromDto(TenantUpdateRequest dto, @MappingTarget Tenant entity);
}