package com.faust0z.B2BSystem.staffing;

import java.util.Optional;

public interface StaffInternalApi {
    Optional<Boolean> getStaffStatus(String identificationNumber);
}
