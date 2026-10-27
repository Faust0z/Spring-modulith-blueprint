package com.faust0z.B2BSystem.tenant.web;

import com.faust0z.B2BSystem.core.RequireAdmin;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import com.faust0z.B2BSystem.tenant.web.BillingCycleController;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayFirstNonDemoRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/billing-cycles")
@Tag(name = "Billing Cycles", description = "Gestión de ciclos de facturación")
class BillingCycleController {

    private final BillingCycleService billingCycleService;

    @Operation(summary = "Obtener todos los ciclos de facturación paginados")
    @ApiResponse(responseCode = "200", description = "Lista paginada de ciclos de facturación")
    @GetMapping
    Page<BillingCycleResponse> findAll(@ParameterObject Pageable pageable) {
        return billingCycleService.findAll(pageable);
    }

    @Operation(summary = "Obtener un ciclo de facturación por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Ciclo de facturación encontrado"),
            @ApiResponse(responseCode = "404", description = "Ciclo de facturación no encontrado")})
    @RequireAdmin
    @GetMapping("/{billingCycleId}")
    BillingCycleResponse findById(@PathVariable UUID billingCycleId) {
        return billingCycleService.findById(billingCycleId);
    }

    @Operation(summary = "Liquidar pago de un ciclo existente", responses = {
            @ApiResponse(responseCode = "200", description = "Pago liquidado correctamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida"),
            @ApiResponse(responseCode = "404", description = "Ciclo no encontrado")})
    @RequireAdmin
    @PostMapping("/pay")
    @ResponseStatus(HttpStatus.OK)
    BillingCycleResponse payCycle(@Valid @RequestBody BillingCyclePayRequest request) {
        try {
            TenantContext.setTenantId(request.tenantId());
            return billingCycleService.payCycle(request);
        } finally {
            TenantContext.clear();
        }
    }

    @Operation(summary = "Registrar primer pago de un tenant", responses = {
            @ApiResponse(responseCode = "200", description = "Primer pago registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida"),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado")})
    @RequireAdmin
    @PostMapping("/pay-first")
    @ResponseStatus(HttpStatus.OK)
    BillingCycleResponse payFirstCycle(@Valid @RequestBody BillingCyclePayFirstNonDemoRequest request) {
        try {
            TenantContext.setTenantId(request.tenantId());
            return billingCycleService.payFirstCycle(request);
        } finally {
            TenantContext.clear();
        }
    }
}
