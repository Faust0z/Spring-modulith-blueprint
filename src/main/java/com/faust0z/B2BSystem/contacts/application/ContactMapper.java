package com.faust0z.B2BSystem.contacts.application;

import com.faust0z.B2BSystem.contacts.domain.ContactEntity;
import com.faust0z.B2BSystem.contacts.web.ContactCreateRequest;
import com.faust0z.B2BSystem.contacts.web.ContactUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContactMapper {
    ContactEntity toEntity(ContactCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateContactFromRequest(ContactUpdateRequest request, @MappingTarget ContactEntity entity);
}
