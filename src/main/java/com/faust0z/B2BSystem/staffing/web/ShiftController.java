package com.faust0z.B2BSystem.staffing.web;

import com.faust0z.B2BSystem.core.RequirePinAuthorization;
import com.faust0z.B2BSystem.staffing.application.ShiftService;
import com.faust0z.B2BSystem.staffing.domain.ShiftsHoursResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/staff/shifts")
@Tag(name = "Staff Shifts", description = "Staff shift management")
public class ShiftController {

    private final ShiftService shiftService;

    @Operation(summary = "Get all shifts")
    @ApiResponse(responseCode = "200", description = "Shifts retrieved successfully")
    @GetMapping
    public Page<ShiftResponse> getAllShifts(@ParameterObject @Valid ShiftFilterRequest filters,
                                    @ParameterObject @PageableDefault(size = 20, sort = "clockInAt") Pageable pageable) {
        return shiftService.getAllShifts(pageable, filters);
    }

    @Operation(summary = "Get hour count for every staff member in desired period range (max 182 days)")
    @ApiResponse(responseCode = "200", description = "Hours worked by each staff member in the range")
    @GetMapping("/hours")
    List<ShiftsHoursResponse> getShiftsHoursSummary(@RequestParam(name = "startDate") LocalDate startDate,
                                                     @RequestParam(name = "endDate") LocalDate endDate) {
        return shiftService.getShiftsHoursSummary(startDate, endDate);
    }

    @Operation(summary = "Process a staff shift")
    @ApiResponse(responseCode = "201", description = "Shift registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public ShiftResponse processShift(@Valid @RequestBody ShiftProcessRequest request) {
        return shiftService.processShift(request);
    }

    @Operation(summary = "Void a shift")
    @ApiResponse(responseCode = "200", description = "Shift voided successfully")
    @RequirePinAuthorization
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{shiftId}/void")
    public void voidShift(@PathVariable UUID shiftId) {
        shiftService.voidShift(shiftId);
    }
}
