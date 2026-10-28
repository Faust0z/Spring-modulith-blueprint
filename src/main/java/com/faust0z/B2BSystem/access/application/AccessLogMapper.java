package com.faust0z.B2BSystem.access.application;

import com.faust0z.B2BSystem.access.domain.AccessLogEntity;
import com.faust0z.B2BSystem.access.domain.AccessLogSummary;
import com.faust0z.B2BSystem.access.web.AccessLogCreateRequest;
import com.faust0z.B2BSystem.access.web.AccessLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface AccessLogMapper {

    @Mapping(target = "memberName", ignore = true)
    @Mapping(target = "accessUntil", ignore = true)
    @Mapping(target = "memberDebtAmount", ignore = true)
    AccessLogResponse toResponse(AccessLogEntity entity);

    @Mapping(target = "memberName", expression = "java(summary.firstName() + \" \" + summary.lastName())")
    @Mapping(target = "accessUntil", source = "accessUntil")
    @Mapping(target = "memberDebtAmount", source = "memberDebtAmount")
    AccessLogResponse toDetails(AccessLogSummary summary);

    AccessLogEntity toEntity(AccessLogCreateRequest request);
}