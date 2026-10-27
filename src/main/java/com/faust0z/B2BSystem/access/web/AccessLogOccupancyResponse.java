package com.faust0z.B2BSystem.access.web;

import com.faust0z.B2BSystem.access.domain.OccupancyDetail;

import java.util.List;

public record AccessLogOccupancyResponse(
        List<OccupancyDetail> branches
) {
}
