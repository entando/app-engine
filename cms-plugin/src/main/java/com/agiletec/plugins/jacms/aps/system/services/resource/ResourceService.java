/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jacms.aps.system.services.resource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.agiletec.aps.system.common.IManager;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.category.CategoryUtilizer;
import com.agiletec.aps.system.services.group.GroupUtilizer;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.AbstractResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceDto;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import java.util.Optional;
import java.util.stream.Collectors;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.entando.entando.aps.system.services.DtoBuilder;
import org.entando.entando.aps.system.services.component.IComponentDto;
import org.entando.entando.aps.system.services.component.IComponentUsageService;
import org.entando.entando.aps.system.services.IDtoBuilder;
import org.entando.entando.aps.system.services.category.CategoryServiceUtilizer;
import org.entando.entando.aps.system.services.group.GroupServiceUtilizer;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.springframework.beans.factory.annotation.Autowired;

public class ResourceService implements IResourceService,
        GroupServiceUtilizer<ResourceDto>, CategoryServiceUtilizer<ResourceDto>, IComponentUsageService {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(this.getClass());
    
    public static final String TYPE_ASSET = ComponentUsageEntity.TYPE_ASSET;

    private IResourceManager resourceManager;
    private IDtoBuilder<ResourceInterface, ResourceDto> dtoBuilder;
    private List<? extends ResourceServiceUtilizer> resourceServiceUtilizers = new ArrayList<>();
    
    @Autowired
    public ResourceService(IResourceManager resourceManager, List<? extends ResourceServiceUtilizer> resourceServiceUtilizers) {
        this.resourceManager = resourceManager;
        this.resourceServiceUtilizers = resourceServiceUtilizers;
    }

    public IResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    protected IDtoBuilder<ResourceInterface, ResourceDto> getDtoBuilder() {
        return dtoBuilder;
    }

    public void setDtoBuilder(IDtoBuilder<ResourceInterface, ResourceDto> dtoBuilder) {
        this.dtoBuilder = dtoBuilder;
    }

    @PostConstruct
    public void setUp() {
        this.setDtoBuilder(new DtoBuilder<ResourceInterface, ResourceDto>() {

            @Override
            protected ResourceDto toDto(ResourceInterface src) {
                ResourceDto resourceDto = new ResourceDto(((AbstractResource) src));
                return resourceDto;
            }
        });
    }

    @Override
    public String getManagerName() {
        return ((IManager) this.getResourceManager()).getName();
    }

    @Override
    public List<ResourceDto> getGroupUtilizer(String groupCode) {
        try {
            List<String> resourcesId = ((GroupUtilizer<String>) this.getResourceManager()).getGroupUtilizers(groupCode);
            return this.buildDtoList(resourcesId);
        } catch (EntException ex) {
            logger.error("Error loading resource references for group {}", groupCode, ex);
            throw new RestServerError("Error loading resource references for group", ex);
        }
    }

    @Override
    public List<ResourceDto> getCategoryUtilizer(String categoryCode) {
        try {
            List<String> resourcesId = ((CategoryUtilizer) this.getResourceManager())
                    .getCategoryUtilizers(categoryCode);
            return this.buildDtoList(resourcesId);
        } catch (EntException ex) {
            logger.error("Error loading resource references for category {}", categoryCode, ex);
            throw new RestServerError("Error loading resource references for category", ex);
        }
    }

    private List<ResourceDto> buildDtoList(List<String> idList) {
        List<ResourceDto> dtoList = new ArrayList<>();
        if (null != idList) {
            idList.stream().forEach(i -> {
                try {
                    dtoList.add(this.getDtoBuilder().convert(this.getResourceManager().loadResource(i)));
                } catch (EntException e) {
                    logger.error("error loading {}", i, e);

                }
            });
        }
        return dtoList;
    }

    @Override
    public Optional<IComponentDto> getComponentDto(String code) throws EntException {
        return Optional.ofNullable(this.resourceManager.loadResource(code))
                .map(c -> this.getDtoBuilder().convert(c));
    }

    @Override
    public boolean exists(String code) throws EntException {
        return resourceManager.exists(null, code);
    }

    @Override
    public String getObjectType() {
        return TYPE_ASSET;
    }

    @Override
    public void deleteComponent(String componentCode) {
        try {
            ResourceInterface resource = this.getResourceManager().loadResource(componentCode);
            if (null != resource) {
                this.resourceManager.deleteResource(resource);
            }
        } catch (EntException ex) {
            logger.error("Error deleting resource {}", componentCode, ex);
            throw new RestServerError("Error deleting resource", ex);
        }
    }

    @Override
    public Integer getComponentUsage(String componentCode) {
        RestListRequest request = new RestListRequest();
        request.setPageSize(-1); // get all elements
        PagedMetadata<ComponentUsageEntity> entities = this.getComponentUsageDetails(componentCode, request);
        return entities.getTotalItems();
    }

    @Override
    public PagedMetadata<ComponentUsageEntity> getComponentUsageDetails(String componentCode, RestListRequest restListRequest) {
        List<ComponentUsageEntity> components = new ArrayList<>();
        for (var utilizer : this.resourceServiceUtilizers) {
            List<IComponentDto> objects = utilizer.getResourceUtilizer(componentCode);
            List<ComponentUsageEntity> utilizerForService = objects.stream()
                    .map(o -> o.buildUsageEntity()).collect(Collectors.toList());
            components.addAll(utilizerForService);
        }
        List<ComponentUsageEntity> sublist = restListRequest.getSublist(components);
        PagedMetadata<ComponentUsageEntity> usageEntries = new PagedMetadata<>(restListRequest, components.size());
        usageEntries.setBody(sublist);
        return usageEntries;
    }

}
