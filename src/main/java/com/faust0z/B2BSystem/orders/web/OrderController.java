package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.application.OrderService;
import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Get all orders")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @GetMapping
    Page<OrderResponse> getAllOrders(@ParameterObject OrderFilterRequest filters,
                                     @ParameterObject @PageableDefault(size = 20, sort = "paidAt") Pageable pageable) {
        return orderService.getAllOrders(pageable, filters);
    }

    @Operation(summary = "Create a new order")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    OrderResponse createOrder(@RequestBody @Valid OrderCreateRequest request) {
        return orderService.createOrder(request);
    }

    @Operation(summary = "Void an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order voided successfully"),
            @ApiResponse(responseCode = "400", description = "Order already voided")
    })
    @PatchMapping("/{orderId}/void")
    OrderResponse voidOrder(@PathVariable UUID orderId) {
        return orderService.voidOrder(orderId);
    }
}
