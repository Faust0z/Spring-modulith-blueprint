package com.faust0z.B2BSystem.orders.application;

import com.faust0z.B2BSystem.orders.domain.InvoiceEntity;
import com.faust0z.B2BSystem.orders.web.InvoiceCreateRequest;
import com.faust0z.B2BSystem.orders.web.InvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    InvoiceResponse toResponse(InvoiceEntity invoiceEntity);

    InvoiceEntity toEntity(InvoiceCreateRequest request);
}
