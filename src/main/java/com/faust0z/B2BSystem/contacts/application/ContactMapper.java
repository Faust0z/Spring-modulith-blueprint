package com.faust0z.B2BSystem.contacts.application;

import com.faust0z.B2BSystem.contacts.domain.ContactEntity;
import com.faust0z.B2BSystem.contacts.domain.ContactSummary;
import com.faust0z.B2BSystem.contacts.web.ContactCreateRequest;
import com.faust0z.B2BSystem.contacts.web.ContactResponse;
import com.faust0z.B2BSystem.contacts.web.ContactUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContactMapper {

    @Mapping(target = "fullName", expression = "java(entity.getFirstName() + \" \" + entity.getLastName())")
    @Mapping(target = "canAccess", expression = "java(entity.getDeletedAt() == null && entity.getAccessUntil() != null && entity.getAccessUntil().isAfter(Instant.now()))")
    @Mapping(target = "isActive", expression = "java(entity.getDeletedAt() == null)")
    ContactResponse toResponse(ContactEntity entity);

    @Mapping(target = "fullName", expression = "java(summary.firstName() + \" \" + summary.lastName())")
    @Mapping(target = "canAccess", expression = "java(summary.accessUntil() != null && summary.accessUntil().isAfter(Instant.now()))")
    @Mapping(target = "isActive", expression = "java(true)")
    ContactResponse toDetails(ContactSummary summary);

    ContactEntity toEntity(ContactCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateContactFromRequest(ContactUpdateRequest request, @MappingTarget ContactEntity entity);
}