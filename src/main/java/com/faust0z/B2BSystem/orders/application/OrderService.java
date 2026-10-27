package com.faust0z.B2BSystem.orders.application;

import com.faust0z.B2BSystem.catalog.ProductInternalApi;
import com.faust0z.B2BSystem.catalog.web.ProductInactiveException;
import com.faust0z.B2BSystem.catalog.web.ProductNotFoundException;
import com.faust0z.B2BSystem.catalog.ProductResponse;
import com.faust0z.B2BSystem.catalog.ServicePlanInternalApi;
import com.faust0z.B2BSystem.catalog.web.ServicePlanDisabledException;
import com.faust0z.B2BSystem.catalog.web.ServicePlanNotFoundException;
import com.faust0z.B2BSystem.catalog.ServicePlanResponse;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.core.DomainRuleException;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.orders.OrderInternalApi;
import com.faust0z.B2BSystem.orders.domain.InvoiceNotFoundException;
import com.faust0z.B2BSystem.orders.domain.InvoiceStatus;
import com.faust0z.B2BSystem.orders.domain.OrderEntity;
import com.faust0z.B2BSystem.orders.domain.OrderItemEntity;
import com.faust0z.B2BSystem.orders.domain.OrderItemType;
import com.faust0z.B2BSystem.orders.domain.OrderNotFoundException;
import com.faust0z.B2BSystem.orders.domain.OrderPlacedEvent;
import com.faust0z.B2BSystem.orders.domain.OrderVoidedEvent;
import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import com.faust0z.B2BSystem.orders.domain.TopOrder;
import com.faust0z.B2BSystem.orders.infrastructure.OrderDetailsViewRepository;
import com.faust0z.B2BSystem.orders.infrastructure.OrderDetailsView;
import com.faust0z.B2BSystem.orders.infrastructure.OrderItemRepository;
import com.faust0z.B2BSystem.orders.infrastructure.OrderRepository;
import com.faust0z.B2BSystem.orders.web.OrderCreateRequest;
import com.faust0z.B2BSystem.orders.web.OrderFilterRequest;
import com.faust0z.B2BSystem.orders.web.OrderItemRequest;
import com.faust0z.B2BSystem.orders.web.OrderResponse;
import com.faust0z.B2BSystem.orders.domain.OrderInternalStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements OrderInternalApi {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDetailsViewRepository orderDetailsViewRepository;
    private final ProductInternalApi productInternalApi;
    private final ServicePlanInternalApi servicePlanInternalApi;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final LocationInternalApi branchInternalApi;
    private final ContactInternalApi contactInternalApi;

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable, OrderFilterRequest filters) {
        log.debug("Getting orders | filters={}", filters);

        List<Specification<OrderDetailsView>> specs = new ArrayList<>();

        if (filters.contactId() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("contactId"), filters.contactId()));
        }

        if (filters.locationId() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("locationId"), filters.locationId()));
        }

        if (filters.paymentMethod() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("paymentMethod"), filters.paymentMethod()));
        }

        if (filters.isVoided() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("isVoided"), filters.isVoided()));
        }

        if (filters.paidAtBegin() != null && filters.paidAtEnd() != null) {
            specs.add((root, query, cb) ->
                    cb.between(root.get("paidAt"), filters.paidAtBegin(), filters.paidAtEnd()));
        } else if (filters.paidAtBegin() != null) {
            specs.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("paidAt"), filters.paidAtBegin()));
        } else if (filters.paidAtEnd() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("paidAt"), filters.paidAtEnd()));
        }

        return orderDetailsViewRepository.findAll(Specification.allOf(specs), pageable).map(OrderDetailsView::toResponse);
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        log.debug("Processing order | contactId={} | itemCount={}", request.contactId(), request.items().size());

        if (request.contactId() != null) {
            contactInternalApi.validateContactActive(request.contactId());
        }
        branchInternalApi.validateLocationActive(request.locationId());

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setTenantId(TenantContext.getTenantId());
        orderEntity.setContactId(request.contactId());
        orderEntity.setLocationId(request.locationId());
        orderEntity.setPaymentMethod(request.paymentMethod());
        orderEntity.setPaidAt(Instant.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            if (itemRequest.itemType() == OrderItemType.SERVICE_PLAN && itemRequest.servicePlanId() != null) {
                var plan = servicePlanInternalApi.findById(itemRequest.servicePlanId())
                        .orElseThrow(ServicePlanNotFoundException::new);
                if (!plan.isActive()) throw new ServicePlanDisabledException();

                BigDecimal itemTotal = plan.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
                totalAmount = totalAmount.add(itemTotal);

                OrderItemEntity orderItem = new OrderItemEntity();
                orderItem.setTenantId(TenantContext.getTenantId());
                orderItem.setOrderId(orderEntity.getId());
                orderItem.setItemType(OrderItemType.SERVICE_PLAN);
                orderItem.setServicePlanId(plan.id());
                orderItem.setName(plan.name());
                orderItem.setQuantity(itemRequest.quantity());
                orderItem.setUnitPrice(plan.price());
                orderItem.setTotalPrice(itemTotal);
                orderItem.setOrder(orderEntity);

                orderEntity.getItems().add(orderItem);
            } else if (itemRequest.itemType() == OrderItemType.PRODUCT && itemRequest.productId() != null) {
                var product = productInternalApi.findById(itemRequest.productId())
                        .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));
                if (!product.isActive()) throw new ProductInactiveException(product.id());

                BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
                totalAmount = totalAmount.add(itemTotal);

                OrderItemEntity orderItem = new OrderItemEntity();
                orderItem.setTenantId(TenantContext.getTenantId());
                orderItem.setOrderId(orderEntity.getId());
                orderItem.setItemType(OrderItemType.PRODUCT);
                orderItem.setProductId(product.id());
                orderItem.setName(product.name());
                orderItem.setQuantity(itemRequest.quantity());
                orderItem.setUnitPrice(product.price());
                orderItem.setTotalPrice(itemTotal);
                orderItem.setOrder(orderEntity);

                orderEntity.getItems().add(orderItem);
            }
        }

        orderEntity.setTotalAmount(totalAmount);
        orderEntity = orderRepository.saveAndFlush(orderEntity);

        applicationEventPublisher.publishEvent(new OrderPlacedEvent(
                orderEntity.getContactId(), orderEntity.getTotalAmount(), orderEntity.getPaidAt()));

        log.info("Order created | orderId={} | contactId={} | amount={}", orderEntity.getId(), orderEntity.getContactId(), orderEntity.getTotalAmount());
        return orderDetailsViewRepository.findById(orderEntity.getId()).map(OrderDetailsView::toResponse).orElseThrow();
    }

    @Transactional
    public OrderResponse voidOrder(UUID orderId) {
        log.debug("Voiding order | orderId={}", orderId);

        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        if (orderEntity.getIsVoided()) {
            throw new DomainRuleException("Este pedido ya se encuentra anulado", "Error al anular el pedido", HttpStatus.BAD_REQUEST);
        }

        orderEntity.setIsVoided(true);
        OrderEntity voidedOrder = orderRepository.saveAndFlush(orderEntity);

        applicationEventPublisher.publishEvent(new OrderVoidedEvent(voidedOrder.getId(), voidedOrder.getContactId()));

        log.info("Order voided | orderId={} | contactId={}", voidedOrder.getId(), voidedOrder.getContactId());
        return orderDetailsViewRepository.findById(voidedOrder.getId()).map(OrderDetailsView::toResponse).orElseThrow();
    }

    @Override
    public Map<UUID, OrderInternalStats> getOrderStats(Set<UUID> locationIds, Instant from, Instant to) {
        List<OrderDetailsView> orders = orderDetailsViewRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("paidAt"), from, to));
            predicates.add(cb.isFalse(root.get("isVoided")));
            predicates.add(root.get("locationId").in(locationIds));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        return orders.stream()
                .collect(Collectors.groupingBy(
                        OrderDetailsView::getLocationId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long count = list.size();
                                    BigDecimal total = list.stream()
                                            .map(OrderDetailsView::getTotalAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return new OrderInternalStats(count, total);
                                }
                        )
                ));
    }

    @Override
    public Map<UUID, List<TopOrder>> getTopOrders(Set<UUID> locationIds, Instant from, Instant to) {
        List<OrderDetailsView> orders = orderDetailsViewRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("paidAt"), from, to));
            predicates.add(cb.isFalse(root.get("isVoided")));
            predicates.add(root.get("locationId").in(locationIds));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        return orders.stream()
                .collect(Collectors.groupingBy(
                        OrderDetailsView::getLocationId,
                        Collectors.mapping(
                                order -> new TopOrder(
                                        order.getLocationId(),
                                        order.getLocationName(),
                                        order.getTotalAmount(),
                                        1L
                                ),
                                Collectors.toList()
                        )
                ));
    }
}
