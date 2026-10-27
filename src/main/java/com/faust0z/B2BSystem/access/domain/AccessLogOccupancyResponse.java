package com.faust0z.B2BSystem.access.domain;

import java.util.List;

record AccessLogOccupancyResponse(
        List<OccupancyDetail> branches
) {
}
