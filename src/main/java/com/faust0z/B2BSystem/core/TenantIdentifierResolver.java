package com.faust0z.B2BSystem.core;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    private static final UUID BOOTSTRAP_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID currentTenant = TenantContext.getTenantId();

        if (currentTenant == null) {
            log.debug("No tenant context found. Providing bootstrap tenant ID for Hibernate session.");
            return BOOTSTRAP_TENANT_ID; //TODO: this is necessary for startup, but creates a small backdoor.
        }

        return currentTenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}