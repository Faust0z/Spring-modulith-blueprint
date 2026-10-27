package com.faust0z.B2BSystem.access.web;

import com.faust0z.B2BSystem.access.application.AccessLogService;
import com.faust0z.B2BSystem.access.web.AccessLogResponse;
import com.faust0z.B2BSystem.access.web.AccessLogCreateRequest;
import com.faust0z.B2BSystem.access.web.AccessLogFilterRequest;
import com.faust0z.B2BSystem.access.web.AccessLogVoidRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/access/logs")
@Tag(name = "Access Logs", description = "Access logs management")
public class AccessLogController {

    private final AccessLogService accessLogService;

    @Operation(summary = "Get all access logs")
    @ApiResponse(responseCode = "200", description = "Access logs obtained successfully")
    @GetMapping
    Page<AccessLogResponse> getAllAccessLogs(@ParameterObject @Valid AccessLogFilterRequest filters,
                                    @ParameterObject @PageableDefault(size = 20, sort = "scannedAt") Pageable pageable) {
        return accessLogService.getAllAccessLogs(pageable, filters);
    }

    @Operation(summary = "Create a new access log")
    @ApiResponse(responseCode = "201", description = "Access log registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid access log")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccessLogResponse createAccessLog(@Valid @RequestBody AccessLogCreateRequest request) {
        return accessLogService.createAccessLog(request);
    }

    @Operation(summary = "Void an access log")
    @ApiResponse(responseCode = "200", description = "Access log voided successfully")
    @PatchMapping("/{accessLogId}/void")
    AccessLogResponse voidEntry(@PathVariable UUID accessLogId, @ParameterObject @Valid AccessLogVoidRequest request) {
        return accessLogService.voidEntryLog(accessLogId, request);
    }
}
