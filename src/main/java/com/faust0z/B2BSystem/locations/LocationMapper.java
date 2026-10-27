package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.locations.LocationResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface LocationMapper {

    @Mapping(target = "deletedAt", source = "deletedAt")
    LocationResponse toResponse(Location entity);

    Location toEntity(LocationCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(LocationUpdateRequest request, @MappingTarget Location entity);
}