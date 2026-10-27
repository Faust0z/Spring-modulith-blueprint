package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.staffing.application.StaffService;
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
@RequestMapping("/v1/staff")
@Tag(name = "Staff", description = "Staff management")
public class StaffController {

    private final StaffService staffService;

    @Operation(summary = "Get all staff")
    @ApiResponse(responseCode = "200", description = "Staff retrieved successfully")
    @GetMapping
    public List<StaffResponse> getStaff() {
        return staffService.getStaff();
    }

    @Operation(summary = "Create a staff member")
    @ApiResponse(responseCode = "201", description = "Staff member created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public StaffResponse createStaff(@Valid @RequestBody StaffCreateRequest request) {
        return staffService.createStaff(request);
    }

    @Operation(summary = "Update a staff member")
    @ApiResponse(responseCode = "200", description = "Staff member updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PatchMapping("/{staffId}")
    public StaffResponse updateStaff(@PathVariable UUID staffId, @Valid @RequestBody StaffUpdateRequest request) {
        return staffService.updateStaff(staffId, request);
    }

    @Operation(summary = "Update a staff member status -> active/inactive")
    @ApiResponse(responseCode = "200", description = "Staff status updated successfully")
    @PatchMapping("/{staffId}/status")
    public StaffResponse toggleStaffStatus(@PathVariable UUID staffId) {
        return staffService.toggleStaffStatus(staffId);
    }
}
