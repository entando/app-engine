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
package org.entando.entando.plugins.jacms.aps.system.services.page;

import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.ContentUtilizer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.IComponentDto;
import org.entando.entando.aps.system.services.IDtoBuilder;
import org.entando.entando.aps.system.services.page.model.PageDto;
import org.entando.entando.plugins.jacms.aps.system.services.content.ContentServiceUtilizer;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.web.component.ComponentUsageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author E.Santoboni
 */
public class CmsPageServiceWrapper implements ContentServiceUtilizer<PageDto> {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(CmsPageServiceWrapper.class);

    @Autowired
    private IDtoBuilder<IPage, PageDto> dtoBuilder;

    @Autowired
    private IPageManager pageManager;

    @Autowired
    @Qualifier(JacmsSystemConstants.PAGE_MANAGER_WRAPPER)
    private ContentUtilizer pageManagerWrapper;

    @Autowired
    private ContentServiceUtilizer pageServiceWrapper;
    
    @Override
    public String getManagerName() {
        return this.getPageManagerWrapper().getName();
    }

    @Override
    public List<PageDto> getContentUtilizer(String contentId) {
        try {
            List<IPage> pages = this.getPageManagerWrapper().getContentUtilizers(contentId);
            return this.getDtoBuilder().convert(pages);
        } catch (EntException ex) {
            logger.error("Error loading page references for content {}", contentId, ex);
            throw new RestServerError("Error loading page references for content", ex);
        }
    }
    
    @Override
    public String getObjectType() {
        return "page";
    }
    
    @Override
    public Integer getComponentUsage(String componentCode) {
        return 0;
    }
    
    @Override
    public PagedMetadata<ComponentUsageEntity> getComponentUsageDetails(String componentCode, RestListRequest restListRequest) {
        List<IComponentDto> objects = this.pageServiceWrapper.getContentUtilizer(componentCode);
        String objectName = this.pageServiceWrapper.getObjectType();
        List<ComponentUsageEntity> utilizerForService = objects.stream()
                .map(o -> o.buildUsageEntity(objectName)).collect(Collectors.toList());
        PagedMetadata<ComponentUsageEntity> usageEntries = new PagedMetadata(restListRequest, utilizerForService.size());
        usageEntries.setBody(utilizerForService);
        return usageEntries;
    }

    @Override
    public IComponentDto getComponentDto(String code) {
        return Optional.ofNullable(this.pageManager.getDraftPage(code))
                .map(c -> this.getDtoBuilder().convert(c)).orElse(null);
    }
    
    @Override
    public boolean exists(String code) throws EntException {
        return (null != this.pageManager.getDraftPage(code));
    }

    protected IDtoBuilder<IPage, PageDto> getDtoBuilder() {
        return dtoBuilder;
    }

    public void setDtoBuilder(IDtoBuilder<IPage, PageDto> dtoBuilder) {
        this.dtoBuilder = dtoBuilder;
    }

    protected ContentUtilizer getPageManagerWrapper() {
        return pageManagerWrapper;
    }

    public void setPageManagerWrapper(ContentUtilizer pageManagerWrapper) {
        this.pageManagerWrapper = pageManagerWrapper;
    }

}
