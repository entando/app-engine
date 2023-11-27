package org.entando.entando.aps.system.services.tenants;

import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.init.IInitializerManager.DatabaseMigrationStrategy;
import org.entando.entando.aps.system.init.InitializerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@Slf4j
@Service
public class TenantInitializerService implements ITenantInitializerService {

    private final TenantDataAccessor tenantDataAccessor;
    private final InitializerManager initializerManager;
    private final DatabaseMigrationStrategy defaultDbInitStrategy;

    @Autowired
    public TenantInitializerService(TenantDataAccessor acecssor, InitializerManager im, @Value("${db.migration.strategy:}") String defaultStrategy){
        this.tenantDataAccessor = acecssor;
        this.initializerManager = im;
        this.defaultDbInitStrategy = Optional.ofNullable(defaultStrategy)
                .map(d -> DatabaseMigrationStrategy.valueOf(d.toUpperCase()))
                .orElse(DatabaseMigrationStrategy.DISABLED);
    }

    @Override
    public CompletableFuture<Void> startTenantsInitialization(ServletContext svCtx, InitializationTenantFilter filter){
        return startTenantsInitializationCore(svCtx, filter);
    }

    private CompletableFuture<Void> startTenantsInitializationCore(ServletContext svCtx, InitializationTenantFilter filter){
        Map<String,TenantStatus> statuses = tenantDataAccessor.getTenantStatuses();
        return CompletableFuture.runAsync(() -> manageTenantsInit(statuses, filter, svCtx), Executors.newSingleThreadExecutor());
    }


    private void manageTenantsInit(Map<String, TenantStatus> statuses, InitializationTenantFilter filter, ServletContext svCtx) {
        log.info("Start asynch initialization for not mandatory tenants");
        long startTenants = System.currentTimeMillis();

        statuses.entrySet().stream()
                .map(e -> tenantDataAccessor.getTenantConfigs().get(e.getKey()))
                .filter(Objects::nonNull)
                .filter(tc -> wantsAll(filter) || isTypeToFilter(tc, filter))
                .map(tc -> tc.getTenantCode())
                .forEach(tenantCode -> {
            long startTenant = System.currentTimeMillis();
            try {
                statuses.put(tenantCode,TenantStatus.PENDING);

                ApsTenantApplicationUtils.setTenant(tenantCode);
                initDb(tenantCode);

                // without status read refresh cannot work fine
                statuses.put(tenantCode,TenantStatus.READY);

                refreshBeanForTenantCode(svCtx, tenantCode);
                
                this.executePostInitProcesses();

            } catch (Throwable th) {
                statuses.put(tenantCode,TenantStatus.FAILED);
                if(tenantDataAccessor.getTenantConfigs().get(tenantCode).isInitializationAtStartRequired()) {
                    throw new RuntimeException(String.format("Tenant:'%s' Error to initialize a required tenant", tenantCode), th);
                } else {
                    log.warn("Tenant:'{}' Error to initialize non required tenant",tenantCode, th);
                }
            } finally {
                log.info("Initialization of tenant '{}' completed in '{}' ms ", tenantCode, System.currentTimeMillis() - startTenant);
            }
        });
        log.info("End initialization for tenants with filter:'{}' in '{}' ms", filter, System.currentTimeMillis() - startTenants);
    }

    private boolean isTypeToFilter(TenantConfig tc, InitializationTenantFilter filter) {
        return tc.isInitializationAtStartRequired() == InitializationTenantFilter.REQUIRED_INIT_AT_START.equals(filter);
    }

    private boolean wantsAll(InitializationTenantFilter filter){
        return filter == null;
    }

    private void initDb(String tenantCode) throws Exception {
        // compute strategy
        DatabaseMigrationStrategy strategy = tenantDataAccessor.getTenantConfigs().entrySet().stream()
                .filter(e -> StringUtils.equals(e.getKey(), tenantCode))
                .findFirst()
                .flatMap(e -> e.getValue().getDbMigrationStrategy())
                .filter(Objects::nonNull)
                .map(s -> DatabaseMigrationStrategy.valueOf(s.toUpperCase()))
                .orElse(defaultDbInitStrategy);
        Map<String, DataSource> datasources = new HashMap<>();
        datasources.put("portDataSource",tenantDataAccessor.getTenantDatasource(tenantCode));
        datasources.put("servDataSource",tenantDataAccessor.getTenantDatasource(tenantCode));
        this.initializerManager.initTenant(strategy, Optional.of(datasources));
    }
    
    private void executePostInitProcesses() {
        initializerManager.executePostInitProcesses();
    }

    private void refreshBeanForTenantCode(ServletContext svCtx, String tenantCode) throws Throwable {
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(svCtx);

        if(wac != null) {
            BaseConfigManager config = wac.getBean(BaseConfigManager.class);
            config.refreshTenantAware();

            // The Map returned by this method should always return bean names and
            // corresponding bean instances in the order of definition in the backend configuration, as far as possible.
            // the returned map should be a LinkedHashMap
            Map<String, RefreshableBeanTenantAware> toRefresh = wac.getBeansOfType(RefreshableBeanTenantAware.class);
            // the returned spliterator is backed by the LinkedHashMap so, if keys are not modified, it is ordered
            toRefresh.entrySet().stream().filter(e -> !(e.getValue() instanceof BaseConfigManager)).forEach(entry -> {
                log.debug("Tenant:'{}' try to refresh bean:'{}'", tenantCode, entry.getKey());
                try {
                    entry.getValue().refreshTenantAware();
                } catch (Throwable th) {
                    log.error("Tenant:'{}' error refresh bean", tenantCode, th);
                    throw new RuntimeException(String.format("error refresh bean %s", entry.getKey()), th);
                }
            });
        } else {
            throw new RuntimeException(String.format("Tenant:'%s' cannot retrieve WebApplicationContext", tenantCode));
        }
    }

}
