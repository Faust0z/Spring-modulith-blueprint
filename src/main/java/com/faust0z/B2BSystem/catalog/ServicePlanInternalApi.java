package com.faust0z.B2BSystem.catalog;

import com.faust0z.B2BSystem.catalog.ServicePlanResponse;

import java.util.Optional;
import java.util.UUID;

public interface ServicePlanInternalApi {
    ServicePlanResponse createDefaultServicePlan();

    Optional<ServicePlanResponse> findById(UUID id);
}
