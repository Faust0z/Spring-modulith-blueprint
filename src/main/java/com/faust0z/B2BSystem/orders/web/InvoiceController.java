package com.faust0z.B2BSystem.orders.web;

import com.faust0z.B2BSystem.orders.application.InvoiceService;
import com.faust0z.B2BSystem.orders.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders/invoices")
@Tag(name = "Invoices", description = "Order invoice management")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Get invoice by order ID")
    @ApiResponse(responseCode = "200", description = "Invoice found")
    @GetMapping("/{orderId}")
    InvoiceResponse getInvoiceByOrderId(@PathVariable UUID orderId) {
        return invoiceService.getInvoiceByOrderId(orderId);
    }

    @Operation(summary = "Create invoice for an order")
    @ApiResponse(responseCode = "201", description = "Invoice created")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    InvoiceResponse createInvoice(@RequestBody @Valid InvoiceCreateRequest request) {
        return invoiceService.createInvoice(request);
    }
}
