package com.faust0z.B2BSystem.catalog;

import com.faust0z.B2BSystem.catalog.CatalogTemplateResponse;
import com.faust0z.B2BSystem.catalog.CatalogTemplateSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CatalogTemplateInternalApi {
    List<CatalogTemplateSummaryResponse> findActiveSummaries();

    CatalogTemplateResponse findById(UUID routineId);
}
