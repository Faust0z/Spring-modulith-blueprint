package com.faust0z.B2BSystem.orders.application;

import com.faust0z.B2BSystem.orders.domain.OrderEntity;
import com.faust0z.B2BSystem.orders.domain.OrderItemEntity;
import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import com.faust0z.B2BSystem.orders.web.OrderCreateRequest;
import com.faust0z.B2BSystem.orders.web.OrderItemResponse;
import com.faust0z.B2BSystem.orders.web.OrderResponse;
import com.faust0z.B2BSystem.orders.web.OrderVoidRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    OrderResponse toResponse(OrderEntity orderEntity);

    OrderItemResponse toItemResponse(OrderItemEntity orderItemEntity);

    OrderEntity toEntity(OrderCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOrderFromRequest(OrderVoidRequest request, @MappingTarget OrderEntity entity);
}
