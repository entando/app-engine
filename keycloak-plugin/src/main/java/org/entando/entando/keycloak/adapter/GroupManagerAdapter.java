/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.keycloak.adapter;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupManager;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.system.services.tenants.TenantStatus;
import org.entando.entando.keycloak.services.KeycloakService;
import org.entando.entando.keycloak.services.oidc.model.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class GroupManagerAdapter extends GroupManager implements IGroupManager {
    
    @Value("${KEYCLOAK_ENABLE_GROUPS_IMPORT:true}")
    private boolean enableGroupsImport;
    
    @Autowired
    private KeycloakService keycloakService;
    
    @Autowired
    private ITenantManager tenantManager;
    
    @Setter
    private boolean keycloakEnabled;
    
    @Override
    public void initTenantAware() throws Exception {
        this.checkAndLoadKcGroups();
        super.initTenantAware();
    }
    
    private void checkAndLoadKcGroups() {
        if (!enableGroupsImport) {
            log.info("Import of Keyclock groups disabled");
            return;
        }
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        Runnable myRunnable = () -> {
            int counter = 0;
            try {
                while (counter < 10 && !isConditionSatisfied()) {
                    Thread.sleep(30000);
                    counter++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for the condition.");
            }
            if (counter < 10) {
                List<GroupRepresentation> kcGroups = this.keycloakService.listGroups();
                Collection<String> entandoGroupCodes = this.getGroupDAO().loadGroups().keySet();
                kcGroups.stream().forEach(gr -> {
                    String id = gr.getId();
                    if (!entandoGroupCodes.contains(id)) {
                        Group newGroup = new Group();
                        newGroup.setName(id);
                        newGroup.setDescription(gr.getName() + " - KC group");
                        this.getGroupDAO().addGroup(newGroup);
                    }
                });
            }
        };
        ScheduledFuture<?> future = executorService.schedule(myRunnable, 0, TimeUnit.MILLISECONDS);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for the condition.");
        }
        executorService.shutdown();
    }
    
    private boolean isConditionSatisfied() {
        Optional<String> currentTenant = ApsTenantApplicationUtils.getTenant();
        return currentTenant.isEmpty() || this.tenantManager.getStatuses().get(currentTenant.get()).equals(TenantStatus.READY);
    }
    
}
