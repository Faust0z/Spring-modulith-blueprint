package com.faust0z.B2BSystem.tenant.web;

import com.faust0z.B2BSystem.core.RequireAdmin;
import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanService;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanController;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/subscription-plans")
@Tag(name = "Subscription Plans", description = "Gestión de planes de subscripción")
class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @Operation(summary = "Obtener todos los planes de suscripción")
    @ApiResponse(responseCode = "200", description = "Lista de planes de suscripción")
    @RequireAdmin
    @GetMapping
    List<SubscriptionPlanResponse> findAll() {
        return subscriptionPlanService.findAll();
    }

    @Operation(summary = "Obtener un plan de suscripción por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Plan de suscripción encontrado"),
            @ApiResponse(responseCode = "404", description = "Plan de suscripción no encontrado")})
    @RequireAdmin
    @GetMapping("/{subscriptionPlanId}")
    SubscriptionPlanResponse findById(@PathVariable UUID subscriptionPlanId) {
        return subscriptionPlanService.findById(subscriptionPlanId);
    }

    @Operation(summary = "Crear un nuevo plan de suscripción", responses = {
            @ApiResponse(responseCode = "201", description = "Plan de suscripción creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida")})
    @RequireAdmin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionPlanResponse create(@Valid @RequestBody SubscriptionPlanCreateRequest request) {
        return subscriptionPlanService.create(request);
    }

    @Operation(summary = "Actualizar un plan de suscripción por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Plan de suscripción actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida"),
            @ApiResponse(responseCode = "404", description = "Plan de suscripción no encontrado")})
    @RequireAdmin
    @PatchMapping("/{subscriptionPlanId}")
    SubscriptionPlanResponse update(@PathVariable UUID subscriptionPlanId, @Valid @RequestBody SubscriptionPlanUpdateRequest request) {
        return subscriptionPlanService.update(subscriptionPlanId, request);
    }
}
