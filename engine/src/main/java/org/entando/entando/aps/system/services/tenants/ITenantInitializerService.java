package org.entando.entando.aps.system.services.tenants;

import java.util.concurrent.CompletableFuture;
import jakarta.servlet.ServletContext;

public interface ITenantInitializerService {

    CompletableFuture<Void> startTenantsInitialization(ServletContext svCtx, InitializationTenantFilter filter);

    enum InitializationTenantFilter {
        REQUIRED_INIT_AT_START,
        NOT_REQUIRED_INIT_AT_START;
    }
}
