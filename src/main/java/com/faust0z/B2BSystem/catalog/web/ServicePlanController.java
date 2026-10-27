package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.catalog.ServicePlanResponse;
import com.faust0z.B2BSystem.catalog.application.ServicePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/catalog/service-plans")
@Tag(name = "service-plans", description = "Service plan management")
public class ServicePlanController {

    private final ServicePlanService servicePlanService;

    @Operation(summary = "Get all service plans")
    @ApiResponse(responseCode = "200", description = "Service plans retrieved successfully")
    @GetMapping
    public List<ServicePlanResponse> getAllServicePlans() {
        return servicePlanService.getAllServicePlans();
    }

    @Operation(summary = "Create a service plan")
    @ApiResponse(responseCode = "201", description = "Service plan created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ServicePlanResponse createServicePlan(@Valid @RequestBody ServicePlanCreateRequest request) {
        return servicePlanService.createServicePlan(request);
    }

    @Operation(summary = "Update a service plan")
    @ApiResponse(responseCode = "200", description = "Service plan updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PatchMapping("/{servicePlanId}")
    public ServicePlanResponse updateServicePlan(@PathVariable UUID servicePlanId, @Valid @RequestBody ServicePlanUpdateRequest request) {
        return servicePlanService.updateServicePlan(servicePlanId, request);
    }

    @Operation(summary = "Update a service plan status -> active/inactive")
    @ApiResponse(responseCode = "200", description = "Service plan status updated successfully")
    @PatchMapping("/{servicePlanId}/status")
    public ServicePlanResponse toggleServicePlanStatus(@PathVariable UUID servicePlanId) {
        return servicePlanService.toggleServicePlanStatus(servicePlanId);
    }
}
