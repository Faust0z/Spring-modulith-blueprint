package com.faust0z.B2BSystem.staffing.domain;

public record ShiftsHoursResponse(
        String staffFullName,
        String staffDni,
        Double totalHours
) {
}
