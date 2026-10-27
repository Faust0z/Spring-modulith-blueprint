package com.faust0z.B2BSystem.staffing.application;

import com.faust0z.B2BSystem.staffing.domain.StaffEntity;
import com.faust0z.B2BSystem.staffing.web.StaffCreateRequest;
import com.faust0z.B2BSystem.staffing.web.StaffResponse;
import com.faust0z.B2BSystem.staffing.web.StaffUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StaffMapper {
    @Mapping(target = "deletedAt", source = "deletedAt")
    StaffResponse toResponse(StaffEntity entity);

    StaffEntity toEntity(StaffCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(StaffUpdateRequest request, @MappingTarget StaffEntity entity);
}
