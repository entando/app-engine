/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.group;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.model.dao.SearcherDaoPaginatedResult;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupUtilizer;
import com.agiletec.aps.system.services.group.IGroupManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.IDtoBuilder;
import org.entando.entando.aps.system.services.group.model.GroupDto;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.entando.entando.aps.system.services.component.IComponentDto;
import org.entando.entando.web.group.model.GroupRequest;
import org.entando.entando.web.group.validator.GroupValidator;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.BeanPropertyBindingResult;

public class GroupService implements IGroupService, ApplicationContextAware {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());
    
    public static final String TYPE_GROUP = "group";
    
    private final IGroupManager groupManager;
    
    private final IDtoBuilder<Group, GroupDto> dtoBuilder;
    
    private final List<? extends GroupServiceUtilizer> groupServiceUtilizers;

    private ApplicationContext applicationContext;
    
    @Autowired
    public GroupService(IGroupManager groupManager, 
            IDtoBuilder<Group, GroupDto> dtoBuilder, List<? extends GroupServiceUtilizer> groupServiceUtilizers) {
        this.groupManager = groupManager;
        this.dtoBuilder = dtoBuilder;
        this.groupServiceUtilizers = groupServiceUtilizers;
    }

    protected IGroupManager getGroupManager() {
        return groupManager;
    }

    protected IDtoBuilder<Group, GroupDto> getDtoBuilder() {
        return dtoBuilder;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PagedMetadata<GroupDto> getGroups(RestListRequest restListReq) {
        try {
            //transforms the filters by overriding the key specified in the request with the correct one known by the dto
            List<FieldSearchFilter> filters = new ArrayList<FieldSearchFilter>(restListReq.buildFieldSearchFilters());
            filters
                    .stream()
                    .filter(i -> i.getKey() != null)
                    .forEach(i -> i.setKey(GroupDto.getEntityFieldName(i.getKey())));

            SearcherDaoPaginatedResult<Group> groups = this.getGroupManager().getGroups(filters);
            List<GroupDto> dtoList = dtoBuilder.convert(groups.getList());

            PagedMetadata<GroupDto> pagedMetadata = new PagedMetadata<>(restListReq, groups);
            pagedMetadata.setBody(dtoList);

            return pagedMetadata;
        } catch (Throwable t) {
            logger.error("error in search groups", t);
            throw new RestServerError("error in search groups", t);
        }
    }

    @Override
    public GroupDto getGroup(String groupCode) {
        Group group = this.getGroupManager().getGroup(groupCode);
        if (null == group) {
            logger.warn("no group found with code {}", groupCode);
            throw new ResourceNotFoundException(GroupValidator.ERRCODE_GROUP_NOT_FOUND, TYPE_GROUP, groupCode);
        }
        GroupDto dto = this.getDtoBuilder().convert(group);
        dto.setReferences(this.getReferencesInfo(group));
        return dto;
    }
    
    @Override
    public Optional<IComponentDto> getComponentDto(String code) {
        return Optional.ofNullable(this.getGroupManager().getGroup(code))
                .map(g -> this.getDtoBuilder().convert(g));
    }

    @Override
    public boolean exists(String groupCode) {
        return this.getGroupManager().getGroup(groupCode) != null;
    }


    @Override
    public PagedMetadata<?> getGroupReferences(String groupCode, String managerName, RestListRequest restRequest) {
        Group group = this.getGroupManager().getGroup(groupCode);
        if (null == group) {
            logger.warn("no group found with code {}", groupCode);
            throw new ResourceNotFoundException(GroupValidator.ERRCODE_GROUP_NOT_FOUND, TYPE_GROUP, groupCode);
        }
        GroupServiceUtilizer<?> utilizer = this.getGroupServiceUtilizer(managerName);
        if (null == utilizer) {
            logger.warn("no references found for {}", managerName);

            throw new ResourceNotFoundException(GroupValidator.ERRCODE_GROUP_REFERENCES, "reference", managerName);
        }
        List<?> dtoList = utilizer.getGroupUtilizer(groupCode);
        List<?> subList = restRequest.getSublist(dtoList);
        int size = 0;
        if (dtoList != null) {
            size = dtoList.size();
        }
        SearcherDaoPaginatedResult<?> pagedResult = new SearcherDaoPaginatedResult(size, subList);
        PagedMetadata<Object> pagedMetadata = new PagedMetadata<>(restRequest, pagedResult);
        pagedMetadata.setBody((List<Object>) subList);
        return pagedMetadata;
    }

    @Override
    public GroupDto updateGroup(String groupCode, String descr) {
        Group group = this.getGroupManager().getGroup(groupCode);
        if (null == group) {
            throw new ResourceNotFoundException(GroupValidator.ERRCODE_GROUP_NOT_FOUND, TYPE_GROUP, groupCode);
        }
        group.setDescription(descr);
        try {
            this.getGroupManager().updateGroup(group);
            return this.getDtoBuilder().convert(group);
        } catch (EntException e) {
            logger.error("Error updating group {}", groupCode, e);
            throw new RestServerError("error in update group", e);
        }
    }

    @Override
    public GroupDto addGroup(GroupRequest groupRequest) {

        Group group = this.createGroup(groupRequest);
        return this.checkForExistenceOrThrowValidationConflictException(group)
                .map(groupDto -> this.saveGroup(group, groupDto))
                .orElseGet(() -> dtoBuilder.convert(group));
    }

    /**
     *
     */
    private GroupDto saveGroup(Group group, GroupDto groupDto) {
        try {
            getGroupManager().addGroup(group);
            return groupDto;
        } catch (EntException e) {
            logger.error("Error adding group", e);
            throw new RestServerError("error add group", e);
        }
    }

    @Override
    public void removeGroup(String groupName) {
        try {
            Group group = this.getGroupManager().getGroup(groupName);

            BeanPropertyBindingResult validationResult = this.checkGroupForDelete(group);
            if (validationResult.hasErrors()) {
                throw new ValidationConflictException(validationResult);
            }

            if (null != group) {
                this.getGroupManager().removeGroup(group);
            }
        } catch (EntException e) {
            logger.error("Error in delete group {}", groupName, e);
            throw new RestServerError("error in delete group", e);
        }
    }

    @Override
    public Integer getComponentUsage(String groupName) {
        Map<String, Boolean> gri = getReferencesInfo(groupName);
        int totalUsageCount = 0;
        for (String holder : gri.keySet()) {
            try {
                RestListRequest request = new RestListRequest(1, 1);
                totalUsageCount += this.getGroupReferences(groupName, holder, request).getTotalItems();
            } catch (ResourceNotFoundException e) {
                totalUsageCount = 0;
            }
        }
        return totalUsageCount;
    }

    @Override
    public PagedMetadata<ComponentUsageEntity> getComponentUsageDetails(String componentCode,
            RestListRequest restListRequest) {
        List<ComponentUsageEntity> components = new ArrayList<>();
        for (var utilizer : this.groupServiceUtilizers) {
            List<IComponentDto> objects = utilizer.getGroupUtilizer(componentCode);
            List<ComponentUsageEntity> utilizerForService = objects.stream()
                    .map(o -> o.buildUsageEntity()).collect(Collectors.toList());
            components.addAll(utilizerForService);
        }
        List<ComponentUsageEntity> sublist = restListRequest.getSublist(components);
        PagedMetadata<ComponentUsageEntity> usageEntries = new PagedMetadata<>(restListRequest, components.size());
        usageEntries.setBody(sublist);
        return usageEntries;
    }

    protected Group createGroup(GroupRequest groupRequest) {
        Group group = new Group();
        group.setName(groupRequest.getCode());
        group.setDescription(groupRequest.getName());
        return group;
    }

    protected BeanPropertyBindingResult checkGroupForDelete(Group group) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(group, TYPE_GROUP);

        if (null == group) {
            return bindingResult;
        }
        if (Group.FREE_GROUP_NAME.equals(group.getName()) || Group.ADMINS_GROUP_NAME.equals(group.getName())) {
            bindingResult.reject(GroupValidator.ERRCODE_CANNOT_DELETE_RESERVED_GROUP, new String[]{group.getName()},
                    "group.cannot.delete.reserved");
        }
        if (!bindingResult.hasErrors()) {

            Map<String, Boolean> references = this.getReferencesInfo(group);
            if (references.size() > 0) {
                for (Map.Entry<String, Boolean> entry : references.entrySet()) {
                    if (true == entry.getValue().booleanValue()) {

                        bindingResult.reject(GroupValidator.ERRCODE_GROUP_REFERENCES,
                                new Object[]{group.getName(), entry.getKey()}, "group.cannot.delete.references");
                    }
                }
            }
        }

        return bindingResult;
    }

    public Map<String, Boolean> getReferencesInfo(Group group) {
        return this.getReferencesInfo(group.getName());
    }

    public Map<String, Boolean> getReferencesInfo(String groupName) {
        Map<String, Boolean> references = new HashMap<>();
        try {
            String[] defNames = applicationContext.getBeanNamesForType(GroupUtilizer.class);
            for (int i = 0; i < defNames.length; i++) {
                Object service;
                try {
                    service = applicationContext.getBean(defNames[i]);
                } catch (Throwable t) {
                    logger.error("error in hasReferencingObjects", t);
                    service = null;
                }
                if (service != null) {
                    GroupUtilizer<?> groupUtilizer = (GroupUtilizer<?>) service;
                    List<?> utilizers = groupUtilizer.getGroupUtilizers(groupName);
                    if (utilizers != null && !utilizers.isEmpty()) {
                        references.put(groupUtilizer.getName(), true);
                    } else {
                        references.put(groupUtilizer.getName(), false);
                    }
                }
            }
        } catch (EntException ex) {
            logger.error("error loading references for group {}", groupName, ex);
            throw new RestServerError("error in getReferencingObjects ", ex);
        }
        return references;
    }

    private GroupServiceUtilizer<?> getGroupServiceUtilizer(String managerName) {
        Map<String, GroupServiceUtilizer> beans = applicationContext.getBeansOfType(GroupServiceUtilizer.class);
        Optional<GroupServiceUtilizer> defName = beans.values().stream()
                .filter(service -> service.getManagerName().equals(managerName))
                .findFirst();
        if (defName.isPresent()) {
            return defName.get();
        }
        return null;
    }


    /**
     * check if the received Group already exists
     * if it exists with equal name it throws ValidationConflictException
     *
     * @param group the group to validate
     * @return the optional of the dto resulting from the validation, if empty the group has NOT to be saved
     */
    protected Optional<GroupDto> checkForExistenceOrThrowValidationConflictException(Group group) {

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(group, TYPE_GROUP);
        Group savedGroup = this.getGroupManager().getGroup(group.getName());

        // check for idempotemcy
        if (null != savedGroup && savedGroup.getName().equals(group.getName())) {
            if (savedGroup.getDescription().equals(group.getDescription())) {
                bindingResult
                        .reject(GroupValidator.ERRCODE_GROUP_ALREADY_EXISTS, new String[]{group.getName()},
                                "group.exists");
            } else {
                bindingResult
                        .reject(GroupValidator.ERRCODE_ADDING_GROUP_WITH_CONFLICTS, new String[]{group.getName()},
                                "group.exists.conflict");
            }
            throw new ValidationConflictException(bindingResult);
        }

        return Optional.of(dtoBuilder.convert(group));
    }

    @Override
    public String getObjectType() {
        return TYPE_GROUP;
    }

    @Override
    public void deleteComponent(String componentCode) {
        this.removeGroup(componentCode);
    }

}
