package com.faust0z.B2BSystem.core;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static final UUID SYSTEM_SCOPE = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}