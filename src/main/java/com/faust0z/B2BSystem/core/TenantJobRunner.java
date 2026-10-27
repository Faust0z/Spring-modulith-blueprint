package com.faust0z.B2BSystem.core;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class TenantJobRunner {

    private TenantJobRunner() {
    }

    public static void forEach(List<UUID> tenantIds, Consumer<UUID> action) {
        for (UUID tid : tenantIds) {
            TenantContext.setTenantId(tid);
            try {
                action.accept(tid);
            } finally {
                TenantContext.clear();
            }
        }
    }
}