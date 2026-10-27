package com.faust0z.B2BSystem.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Dashboard statistics request")
public record DashboardStatsRequest(
        @Schema(description = "Start date for the statistics range", example = "2024-01-01")
        @NotNull(message = "Start date is required")
        LocalDate start,

        @Schema(description = "End date for the statistics range", example = "2024-01-31")
        @NotNull(message = "End date is required")
        LocalDate end,

        @Schema(description = "Minutes window for occupancy estimate", example = "90")
        int minutes
) {
}
