package com.faust0z.B2BSystem.staffing.domain;

import java.util.UUID;

public interface ShiftsHoursProjection {
    UUID getStaffId();

    Double getTotalHours();
}
