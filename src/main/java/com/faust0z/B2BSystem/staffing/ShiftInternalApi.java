package com.faust0z.B2BSystem.staffing;

import com.faust0z.B2BSystem.staffing.web.ShiftProcessRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftResponse;

public interface ShiftInternalApi {
    ShiftResponse processShift(ShiftProcessRequest request);
}
