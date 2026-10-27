package com.faust0z.B2BSystem.catalog.web;

import com.faust0z.B2BSystem.catalog.application.CatalogTemplateService;
import com.faust0z.B2BSystem.catalog.CatalogTemplateResponse;
import com.faust0z.B2BSystem.catalog.CatalogTemplateSummaryResponse;
import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateCreateRequest;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateFilterRequest;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateUpdateRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/catalog/templates")
@Tag(name = "Catalog Templates", description = "Catalog templates' management")
public class CatalogTemplateController {

    private final CatalogTemplateService catalogTemplateService;

    @Operation(summary = "Get all routines")
    @ApiResponse(responseCode = "200", description = "Rutinas obtenidas correctamente")
    @GetMapping
    Page<CatalogTemplateSummaryResponse> getAllRoutines(@ParameterObject CatalogTemplateFilterRequest filter, @ParameterObject Pageable pageable) {
        return catalogTemplateService.getAllRoutines(filter, pageable);
    }

    @Operation(summary = "Get a single routine by ID")
    @ApiResponse(responseCode = "200", description = "Rutina encontrada")
    @ApiResponse(responseCode = "404", description = "Rutina no encontrada")
    @GetMapping("/{routineId}")
    CatalogTemplateResponse getRoutineById(@PathVariable UUID routineId) {
        return catalogTemplateService.getRoutineById(routineId);
    }

    @Operation(summary = "Create a routine")
    @ApiResponse(responseCode = "201", description = "Rutina creada correctamente")
    @ApiResponse(responseCode = "400", description = "Entrada inválida")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    CatalogTemplateResponse createRoutine(@Valid @RequestBody CatalogTemplateCreateRequest request) {
        return catalogTemplateService.createRoutine(request);
    }

    @Operation(summary = "Update a routine")
    @ApiResponse(responseCode = "200", description = "Rutina actualizada correctamente")
    @ApiResponse(responseCode = "400", description = "Entrada inválida")
    @PatchMapping("/{routineId}")
    CatalogTemplateResponse updateRoutine(@PathVariable UUID routineId, @Valid @RequestBody CatalogTemplateUpdateRequest request) {
        return catalogTemplateService.updateRoutine(routineId, request);
    }

    @Operation(summary = "Update a routine status -> active/inactive")
    @ApiResponse(responseCode = "200", description = "Estado de la rutina actualizado correctamente")
    @PatchMapping("/{routineId}/status")
    CatalogTemplateResponse toggleRoutineStatus(@PathVariable UUID routineId) {
        return catalogTemplateService.toggleRoutineStatus(routineId);
    }
}
