package com.faust0z.B2BSystem.tenant;

import com.faust0z.B2BSystem.tenant.BillingCycleHydratedResponse;
import com.faust0z.B2BSystem.tenant.BillingCycleInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
public interface BillingCycleInternalApi {
    boolean hasValidCycle();

    BillingCycleHydratedResponse computeNextCycleToPay(TenantInternalResponse tenant);
}
