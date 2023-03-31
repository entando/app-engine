package org.entando.entando.aps.system.services.tenants;

import java.util.concurrent.CompletableFuture;
import javax.servlet.ServletContext;

public interface ITenantInitializerService {

    CompletableFuture<Void> startTenantsInitialization(ServletContext svCtx);

    CompletableFuture<Void> startTenantsInitializationWithFilter(ServletContext svCtx, InitializationTenantFilter filter);

    enum InitializationTenantFilter {
        ALL,
        REQUIRED_INIT_AT_START,
        NOT_REQUIRED_INIT_AT_START;
    }
}
