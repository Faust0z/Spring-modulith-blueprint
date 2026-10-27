package com.faust0z.B2BSystem.access.web;

import com.faust0z.B2BSystem.staffing.web.ShiftResponse;

public record ScanResponse(
        ShiftResponse staffShift,
        AccessLogResponse accessLog
) {
}
