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
package com.agiletec.plugins.jacms.apsadmin.system;

import java.util.ArrayList;
import java.util.List;

import org.entando.entando.aps.util.PageUtils;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.apsadmin.system.entity.type.EntityTypeConfigAction;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.ContentModel;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.IContentModelManager;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;

/**
 * @author E.Santoboni
 */
public class ContentTypeConfigAction extends EntityTypeConfigAction {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ContentTypeConfigAction.class);

	@Override
	protected IApsEntity updateEntityOnSession() {
		Content contentType = (Content) super.updateEntityOnSession();
		contentType.setViewPage(this.getViewPageCode());
		if (null != this.getListModelId()) {
			contentType.setListModel(this.getListModelId().toString());
		}
		if (null != this.getDefaultModelId()) {
			contentType.setDefaultModel(this.getDefaultModelId().toString());
		}
		return contentType;
	}

	/**
	 * Return a plain list of the free viewer pages in the portal.
	 *
	 * @return the list of the free viewer pages of the portal.
	 */
	public List<IPage> getFreeViewerPages() {
		IPage root = this.getPageManager().getOnlineRoot();
		List<IPage> pages = new ArrayList<IPage>();
		this.addPages(root, pages);
		return pages;
	}

	private void addPages(IPage page, List<IPage> pages) {
		if (null == page) {
			return;
		}
        PageModel model = this.getPageModelManager().getPageModel(page.getMetadata().getModelCode());
		if (page.getGroup().equals(Group.FREE_GROUP_NAME) && PageUtils.isOnlineFreeViewerPage(page, model, null, this.getWidgetTypeManager())) {
			pages.add(page);
		}
		String[] children = page.getChildrenCodes();
		if (null == children) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			IPage child = this.getPageManager().getOnlinePage(children[i]);
			this.addPages(child, pages);
		}
	}

	/**
	 * Return the list of contentmodel given the content type code.
	 *
	 * @param typeCode The content type code.
	 * @return The Content Models found
	 */
	public List<ContentModel> getContentModels(String typeCode) {
		if (null == typeCode) {
			return new ArrayList<ContentModel>();
		}
		List<ContentModel> models = null;
		try {
			models = this.getContentModelManager().getModelsForContentType(typeCode);
		} catch (Throwable t) {
			_logger.error("Error on extracting models by type  {}", typeCode, t);
			throw new RuntimeException("Error on extracting models by type " + typeCode, t);
		}
		return models;
	}

	public String getViewPageCode() {
		return viewPageCode;
	}

	public void setViewPageCode(String viewPageCode) {
		this.viewPageCode = viewPageCode;
	}

	public Integer getListModelId() {
		return listModelId;
	}

	public void setListModelId(Integer listModelId) {
		this.listModelId = listModelId;
	}

	public Integer getDefaultModelId() {
		return defaultModelId;
	}

	public void setDefaultModelId(Integer defaultModelId) {
		this.defaultModelId = defaultModelId;
	}

	protected IPageManager getPageManager() {
		return pageManager;
	}

	public void setPageManager(IPageManager pageManager) {
		this.pageManager = pageManager;
	}

	protected IContentModelManager getContentModelManager() {
		return contentModelManager;
	}

	public void setContentModelManager(IContentModelManager contentModelManager) {
		this.contentModelManager = contentModelManager;
	}
    
    protected IWidgetTypeManager getWidgetTypeManager() {
        return widgetTypeManager;
    }
    public void setWidgetTypeManager(IWidgetTypeManager widgetTypeManager) {
        this.widgetTypeManager = widgetTypeManager;
    }

    protected IPageModelManager getPageModelManager() {
        return pageModelManager;
    }
    public void setPageModelManager(IPageModelManager pageModelManager) {
        this.pageModelManager = pageModelManager;
    }

	private String viewPageCode;
	private Integer listModelId;
	private Integer defaultModelId;

	private transient IPageManager pageManager;
	private transient IContentModelManager contentModelManager;
	private transient IWidgetTypeManager widgetTypeManager;
	private transient IPageModelManager pageModelManager;
    
}
