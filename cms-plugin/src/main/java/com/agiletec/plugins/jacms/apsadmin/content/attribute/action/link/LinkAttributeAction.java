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
package com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link;

import jakarta.servlet.http.HttpSession;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.ContentRecordVO;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SymbolicLink;
import com.agiletec.plugins.jacms.apsadmin.content.ContentActionConstants;
import com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link.helper.ILinkAttributeActionHelper;
import com.agiletec.plugins.jacms.apsadmin.content.helper.IContentActionHelper;

/**
 * Classe action delegata alla gestione delle operazioni base sugli attributi Link.
 * Le azioni gestite rappresentano ciascuna un entry point dall'interfaccia di redazione contenuto. 
 * @author E.Santoboni
 */
public class LinkAttributeAction extends BaseAction implements ILinkAttributeAction {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(LinkAttributeAction.class);

	@Override
	public String chooseLink() {
		try {
			Content content = this.getContent();
			this.getContentActionHelper().updateEntity(content, this.getRequest());
			this.getLinkAttributeHelper().initSessionParams(this, this.getRequest());
			this.updateContent(content);
		} catch (Throwable t) {
			_logger.error("chooseLink", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	@Override
	public String chooseLinkType() {
		String result = FAILURE;
		SymbolicLink symbolicLink = getSymbolicLink();
		if (null == symbolicLink)
		{
			return SUCCESS;
		}
		switch (symbolicLink.getDestType()) {
			case (SymbolicLink.CONTENT_TYPE):
				result = "configContentLink";
				break;
			case (SymbolicLink.CONTENT_ON_PAGE_TYPE):
                result = "configContentLink";
				break;
			case SymbolicLink.PAGE_TYPE:
				result = "configPageLink";
				break;
			case SymbolicLink.URL_TYPE:
				result = "configUrlLink";
				break;
			default:
				result = "configUrlLink";
				break;
		}
		return result;
	}

	public String configLink() {
		String result = FAILURE;
		int destType = this.getLinkType();
        switch (destType) {
        case (SymbolicLink.CONTENT_TYPE):
        	result = "configContentLink";
            break;
        case (SymbolicLink.CONTENT_ON_PAGE_TYPE):
        	_logger.error("link on contentOnPage unavailable");
        	//ApsSystemUtils.logThrowable(new RuntimeException("Non è possibile selezionare direttamente " +	"il link di contenuto su pagina"), this, "configLink");
            break;
        case SymbolicLink.PAGE_TYPE:
        	result = "configPageLink";
            break;
        case SymbolicLink.URL_TYPE:
        	result = "configUrlLink";
            break;
        default:
        	_logger.error("invalida link type: {}", destType);
            break;
        }
		return result;
	}
	
	@Override
	public String removeLink() {
		try {
			Content content = this.getContent();
			this.getContentActionHelper().updateEntity(content, this.getRequest());
			this.getLinkAttributeHelper().initSessionParams(this, this.getRequest());
			this.getLinkAttributeHelper().removeLink(this.getRequest());
			this.updateContent(content);
		} catch (Throwable t) {
			_logger.error("error in removeLink", t);
			//ApsSystemUtils.logThrowable(t, this, "removeLink");
			return FAILURE;
		}
		return SUCCESS;
	}
	
	public String backToEntryContent() {
		HttpSession session = this.getRequest().getSession();
		String anchorDest = this.getLinkAttributeHelper().buildEntryContentAnchorDest(session);
		this.setEntryContentAnchorDest(anchorDest);
		this.getLinkAttributeHelper().removeSessionParams(session);
		return SUCCESS;
	}
	
	public String getEntryContentAnchorDestFromRemove() {
		StringBuilder buffer = new StringBuilder("contentedit_");
		buffer.append(this.getLangCodeOfLink());
		buffer.append("_");
		if (null != this.getParentAttributeName()) {
			buffer.append(this.getParentAttributeName());
		} else {
			buffer.append(this.getAttributeName());
		}
		return buffer.toString();
	}
	
	public Content getContent() {
		return (Content) this.getRequest().getSession()
				.getAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + this.getContentOnSessionMarker());
	}

	protected void updateContent(Content content) {
		this.getRequest().getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + this.getContentOnSessionMarker(), content);
	}

	public String getContentOnSessionMarker() {
		return _contentOnSessionMarker;
	}
	public void setContentOnSessionMarker(String contentOnSessionMarker) {
		this._contentOnSessionMarker = contentOnSessionMarker;
	}
	
	public SymbolicLink getSymbolicLink() {
		return (SymbolicLink) this.getRequest().getSession().getAttribute(ILinkAttributeActionHelper.SYMBOLIC_LINK_SESSION_PARAM);
	}
	
	public int[] getLinkDestinations() {
		return SymbolicLink.getDestinationTypes();
	}
	
	public IPage getPage(String pageCode) {
		return this.getPageManager().getOnlinePage(pageCode);
	}
	
	public ContentRecordVO getContentVo(String contentId) {
		ContentRecordVO contentVo = null;
		try {
			contentVo = this.getContentManager().loadContentVO(contentId);
		} catch (Throwable t) {
			_logger.error("error in getContentVo", t);
		}
		return contentVo;
	}

	public int getLinkType() {
		return _linkType;
	}
	public void setLinkType(int linkType) {
		this._linkType = linkType;
	}
	
	protected IPageManager getPageManager() {
		return _pageManager;
	}
	public void setPageManager(IPageManager pageManager) {
		this._pageManager = pageManager;
	}
	
	protected IContentManager getContentManager() {
		return _contentManager;
	}
	public void setContentManager(IContentManager contentManager) {
		this._contentManager = contentManager;
	}
	
	@Override
	public String getAttributeName() {
		return _attributeName;
	}
	public void setAttributeName(String attributeName) {
		this._attributeName = attributeName;
	}
	
	@Override
	public String getParentAttributeName() {
		return _parentAttributeName;
	}
	public void setParentAttributeName(String parentAttributeName) {
		this._parentAttributeName = parentAttributeName;
	}
	
	@Override
	public int getElementIndex() {
		return _elementIndex;
	}
	public void setElementIndex(int elementIndex) {
		this._elementIndex = elementIndex;
	}
	
	@Override
	public String getLangCodeOfLink() {
		return _langCode;
	}
	public void setLangCodeOfLink(String langCode) {
		this._langCode = langCode;
	}
	
	public String getEntryContentAnchorDest() {
		if (null == this._entryContentAnchorDest) {
			HttpSession session = this.getRequest().getSession();
			String anchorDest = this.getLinkAttributeHelper().buildEntryContentAnchorDest(session);
			this.setEntryContentAnchorDest(anchorDest);
		}
		return _entryContentAnchorDest;
	}
	protected void setEntryContentAnchorDest(String entryContentAnchorDest) {
		this._entryContentAnchorDest = entryContentAnchorDest;
	}
	
	/**
	 * Restituisce la classe helper della gestione contenuti.
	 * @return La classe helper della gestione contenuti.
	 */
	protected IContentActionHelper getContentActionHelper() {
		return _contentActionHelper;
	}
	
	/**
	 * Setta la classe helper della gestione contenuti.
	 * @param contentActionHelper La classe helper della gestione contenuti.
	 */
	public void setContentActionHelper(IContentActionHelper contentActionHelper) {
		this._contentActionHelper = contentActionHelper;
	}
	
	/**
	 * Restituisce la classe helper della gestione degli attributi di tipo Link.
	 * @return La classe helper degli attributi di tipo Link.
	 */
	protected ILinkAttributeActionHelper getLinkAttributeHelper() {
		return _linkAttributeHelper;
	}
	/**
	 * Setta la classe helper della gestione degli attributi di tipo Link.
	 * @param linkAttributeHelper La classe helper degli attributi di tipo Link.
	 */
	public void setLinkAttributeHelper(ILinkAttributeActionHelper linkAttributeHelper) {
		this._linkAttributeHelper = linkAttributeHelper;
	}
	
	private String _contentOnSessionMarker;
	
	private String _attributeName;
	private String _parentAttributeName;
	private int _elementIndex = -1;
	private String _langCode;
	private int _linkType;
	
	private String _entryContentAnchorDest;
	
	private IPageManager _pageManager;
	private IContentManager _contentManager;
	
	private IContentActionHelper _contentActionHelper;
	private ILinkAttributeActionHelper _linkAttributeHelper;
	
}