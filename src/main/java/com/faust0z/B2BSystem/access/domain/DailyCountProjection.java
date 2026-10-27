package com.faust0z.B2BSystem.access.domain;

import java.time.LocalDate;
import java.util.UUID;

public interface DailyCountProjection {
    UUID getLocationId();

    LocalDate getEntryDate();

    Long getTotal();
}
