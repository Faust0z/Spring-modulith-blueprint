package com.faust0z.B2BSystem.dashboard;

import com.faust0z.B2BSystem.core.RequirePinAuthorization;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics")
public class DashboardController {

    private final com.faust0z.B2BSystem.dashboard.DashboardService dashboardService;

    @Operation(summary = "Get dashboard data")
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully")
    @GetMapping
    public DashboardResponse getDashboard(
            @RequestParam(name = "start") LocalDate start,
            @RequestParam(name = "end") LocalDate end,
            @RequestParam(name = "minutes", defaultValue = "90") int minutes) {
        return dashboardService.compileDashboardData(start, end, minutes);
    }
}
