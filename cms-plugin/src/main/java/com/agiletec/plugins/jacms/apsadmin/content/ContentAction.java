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
package com.agiletec.plugins.jacms.apsadmin.content;

import org.entando.entando.aps.util.PageUtils;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.aps.util.SelectItem;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.ContentUtilizer;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SymbolicLink;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.apsadmin.util.ResourceIconUtil;
import org.apache.commons.lang.StringUtils;
import org.entando.entando.plugins.jacms.aps.util.CmsPageUtil;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;

/**
 * Action principale per la redazione contenuti.
 *
 * @author E.Santoboni
 */
public class ContentAction extends AbstractContentAction {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ContentAction.class);

    private transient IPageManager pageManager;
    private transient IPageModelManager pageModelManager;
    private transient ConfigInterface configManager;
    private transient IResourceManager resourceManager;
    private transient IWidgetTypeManager widgetTypeManager;

    private Map references;

    private String contentId;

    private List<String> extraGroupNames = new ArrayList<>();
    
    private String groupToRemove;

    private boolean copyPublicVersion;

    private ResourceIconUtil resourceIconUtil;

    @Override
    public void validate() {
        Content content = this.updateContentOnSession(true);
        super.validate();
        this.getContentActionHelper().scanEntity(content, this);
    }

    /**
     * Esegue l'azione di edit di un contenuto.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String edit() {
        try {
            Content content = this.getContentManager().loadContent(this.getContentId(), false);
            if (null == content) {
                throw new EntException("Contenuto in edit '" + this.getContentId() + "' nullo!");
            }
            if (!this.isUserAllowed(content)) {
                _logger.info("Utente non abilitato all'editazione del contenuto {}", content.getId());
                return USER_NOT_ALLOWED;
            }
            String marker = buildContentOnSessionMarker(content, ApsAdminSystemConstants.EDIT);
            super.setContentOnSessionMarker(marker);
            this.getRequest().getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + marker, content);
        } catch (Throwable t) {
            _logger.error("error in edit", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Esegue l'azione di copia/incolla di un contenuto.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String copyPaste() {
        try {
            Content content = this.getContentManager().loadContent(this.getContentId(), this.isCopyPublicVersion());
            if (null == content) {
                throw new EntException("Contenuto in copyPaste '" + this.getContentId() + "' nullo ; copia di contenuto pubblico "
                        + this.isCopyPublicVersion());
            }
            if (!this.isUserAllowed(content)) {
                _logger.info("Utente non abilitato all'accesso del contenuto {}", content.getId());
                return USER_NOT_ALLOWED;
            }
            String marker = buildContentOnSessionMarker(content, ApsAdminSystemConstants.PASTE);
            content.setId(null);
            content.setVersion(Content.INIT_VERSION);
            content.setDescription(this.getText("label.copyOf") + " " + content.getDescription());
            content.setCreated(new Date());
            content.setLastModified(null);
            content.setPublished(null);
            content.setFirstEditor(this.getCurrentUser().getUsername());
            content.setLastEditor(null);
            super.setContentOnSessionMarker(marker);
            this.getRequest().getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + marker, content);
        } catch (Throwable t) {
            _logger.error("error in copyPaste", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    public String forwardToEntryContent() {
        return SUCCESS;
    }

    public String configureMainGroup() {
        try {
            Content content = this.updateContentOnSession();
            if (null == content) {
                _logger.warn("Null content on session");
                return FAILURE;
            }
            if (/* null == content.getId() && */null == content.getMainGroup()) {
                String mainGroup = this.getRequest().getParameter("mainGroup");
                if (mainGroup != null && null != this.getGroupManager().getGroup(mainGroup)) {
                    content.setMainGroup(mainGroup);
                    this.updateContent(content);
                }
            }
        } catch (Throwable t) {
            _logger.error("error in setMainGroup", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Esegue l'azione di associazione di un gruppo al contenuto in fase di
     * redazione.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String joinGroup() {
        try {
            Content content = this.updateContentOnSession();
            for (String extraGroupName : extraGroupNames) {
                Group group = this.getGroupManager().getGroup(extraGroupName);
                if (null != group) {
                    content.addGroup(extraGroupName);
                }
            }
            this.updateContent(content);
            // we want to clear the group selection after the join
            extraGroupNames.clear();
        } catch (Throwable t) {
            _logger.error("error in joinGroup", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Esegue l'azione di rimozione di un gruppo dal contenuto in fase di
     * redazione.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String removeGroup() {
        try {
            Content content = this.updateContentOnSession();
            // NOTE: previously the extraGroupNames variable was reused
            // also for group removal. It is necessary to have a separate
            // variable for this, in order to avoid conflicting with the
            // joinGroup() method. See EN-2166.
            Group group = this.getGroupManager().getGroup(groupToRemove);
            if (null != group) {
                content.getGroups().remove(group.getName());
                this.updateContent(content);
            }
        } catch (Throwable t) {
            _logger.error("error in removeGroup", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    public String saveAndContinue() {
        try {
            Content currentContent = this.updateContentOnSession();
            if (null != currentContent) {
                String descr = currentContent.getDescription();
                if (StringUtils.isEmpty(descr)) {
                    this.addFieldError("descr", this.getText("error.content.descr.required"));
                } else if (descr.length() > 250) {
                    String[] args = {String.valueOf(250)};
                    this.addFieldError("descr", this.getText("error.content.descr.wrongMaxLength", args));
                } else if (StringUtils.isEmpty(currentContent.getMainGroup())) {
                    this.addFieldError("mainGroup", this.getText("error.content.mainGroup.required"));
                } else {
                    currentContent.setLastEditor(this.getCurrentUser().getUsername());
                    this.getContentManager().saveContent(currentContent);
                }
            }
            this.updateContent(currentContent);
        } catch (Throwable t) {
            _logger.error("error in saveAndContinue", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Esegue l'azione di salvataggio del contenuto in fase di redazione.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String saveContent() {
        return this.saveContent(false);
    }

    /**
     * Esegue l'azione di salvataggio e pubblicazione del contenuto in fase di
     * redazione.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String saveAndApprove() {
        return this.saveContent(true);
    }

    protected String saveContent(boolean approve) {
        try {
            Content currentContent = this.getContent();
            if (null != currentContent) {
                int strutsAction = (null == currentContent.getId()) ? ApsAdminSystemConstants.ADD : ApsAdminSystemConstants.EDIT;
                if (approve) {
                    strutsAction += 10;
                }
                if (!this.getContentActionHelper().isUserAllowed(currentContent, this.getCurrentUser())) {
                    _logger.info("User not allowed to save content {}", currentContent.getId());
                    return USER_NOT_ALLOWED;
                }
                currentContent.setLastEditor(this.getCurrentUser().getUsername());
                if (approve) {
                    this.getContentManager().insertOnLineContent(currentContent);
                } else {
                    this.getContentManager().saveContent(currentContent);
                }
                this.addActivityStreamInfo(currentContent, strutsAction, true);
                this.getRequest().getSession().removeAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX
                        + super.getContentOnSessionMarker());
                this.getRequest().getSession().removeAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_GROUP);
                _logger.info("Saving content {} - Description: '{}' - User: {}", currentContent.getId(), currentContent.getDescription(),
                        this.getCurrentUser().getUsername());
            } else {
                _logger.error("Save/approve NULL content - User: {}", this.getCurrentUser().getUsername());
            }
        } catch (Throwable t) {
            _logger.error("error in saveContent", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Esegue l'azione di rimozione del contenuto pubblico e salvataggio del
     * contenuto in fase di redazione.
     *
     * @return Il codice del risultato dell'azione.
     */
    public String suspend() {
        try {
            Content currentContent = this.updateContentOnSession();
            if (null != currentContent) {
                if (!this.getContentActionHelper().isUserAllowed(currentContent, this.getCurrentUser())) {
                    _logger.info("User not allowed to unpublish content {}", currentContent.getId());
                    return USER_NOT_ALLOWED;
                }
                Map extractedReferences = this.getContentActionHelper().getReferencingObjects(currentContent, this.getRequest());
                if (extractedReferences.size() > 0) {
                    this.setReferences(extractedReferences);
                    return "references";
                }
                this.getContentManager().removeOnLineContent(currentContent);
                this.addActivityStreamInfo(currentContent, (ApsAdminSystemConstants.DELETE + 10), true);
                this.getRequest().getSession().removeAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX
                        + super.getContentOnSessionMarker());
                _logger.info("Content {} suspended - Description: '{}' - Utente: {}", currentContent.getId(), currentContent
                        .getDescription(), this.getCurrentUser().getUsername());
            }
        } catch (Throwable t) {
            _logger.error("error in suspend", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    public int[] getLinkDestinations() {
        return SymbolicLink.getDestinationTypes();
    }

    public IPage getPage(String pageCode) {
        return this.getPageManager().getOnlinePage(pageCode);
    }

    public String getHtmlEditorCode() {
        return this.getConfigManager().getParam("hypertextEditor");
    }

    /**
     * Return the list of the showing pages of the current content on edit
     *
     * @return The list of the showing pages.
     */
    public List<SelectItem> getShowingPageSelectItems() {
        List<SelectItem> pageItems = new ArrayList<>();
        try {
            Content content = this.getContent();
            if (null != content) {
                IPage defaultViewerPage = this.getPageManager().getOnlinePage(content.getViewPage());
                if (defaultViewerPage == null) {
                    return pageItems;
                }
                PageModel model = this.getPageModelManager().getPageModel(defaultViewerPage.getMetadata().getModelCode());
                if (PageUtils.isOnlineFreeViewerPage(defaultViewerPage, model, null, this.getWidgetTypeManager())) {
                    pageItems.add(new SelectItem("", this.getText("label.default")));
                }
                if (null == content.getId()) {
                    return pageItems;
                }
                ContentUtilizer pageManagerWrapper = (ContentUtilizer) ApsWebApplicationUtils.getBean(
                        JacmsSystemConstants.PAGE_MANAGER_WRAPPER, this.getRequest());
                List<IPage> pages = pageManagerWrapper.getContentUtilizers(content.getId());
                for (int i = 0; i < pages.size(); i++) {
                    IPage page = pages.get(i);
                    String pageCode = page.getCode();
                    pageItems.add(new SelectItem(pageCode, super.getTitle(pageCode, page.getTitles())));
                }
            }
        } catch (Throwable t) {
            _logger.error("Error on extracting showing pages", t);
            throw new RuntimeException("Error on extracting showing pages", t);
        }
        return pageItems;
    }

    public String getIconFile(String fileName) {
        return this.getResourceIconUtil().getIconByFilename(fileName);
    }

    public List<String> getResourceMetadataKeys() {
        List<String> keys = new ArrayList<>();
        Map<String, List<String>> mapping = this.getResourceManager().getMetadataMapping();
        if (null != mapping) {
            keys.addAll(mapping.keySet());
            Collections.sort(keys);
        }
        return keys;
    }

    public Map getReferences() {
        return references;
    }

    protected void setReferences(Map references) {
        this.references = references;
    }

    protected IPageManager getPageManager() {
        return pageManager;
    }

    public void setPageManager(IPageManager pageManager) {
        this.pageManager = pageManager;
    }

    protected IPageModelManager getPageModelManager() {
        return pageModelManager;
    }
    
    public void setPageModelManager(IPageModelManager pageModelManager) {
        this.pageModelManager = pageModelManager;
    }

    protected ConfigInterface getConfigManager() {
        return configManager;
    }

    public void setConfigManager(ConfigInterface configManager) {
        this.configManager = configManager;
    }

    protected IResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    protected IWidgetTypeManager getWidgetTypeManager() {
        return widgetTypeManager;
    }
    
    public void setWidgetTypeManager(IWidgetTypeManager widgetTypeManager) {
        this.widgetTypeManager = widgetTypeManager;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public List<String> getExtraGroupNames() {
        return extraGroupNames;
    }

    public void setExtraGroupNames(List<String> extraGroupNames) {
        this.extraGroupNames = extraGroupNames;
    }

    public String getGroupToRemove() {
        return groupToRemove;
    }

    public void setGroupToRemove(String groupToRemove) {
        this.groupToRemove = groupToRemove;
    }

    public boolean isCopyPublicVersion() {
        return copyPublicVersion;
    }

    public void setCopyPublicVersion(boolean copyPublicVersion) {
        this.copyPublicVersion = copyPublicVersion;
    }

    protected ResourceIconUtil getResourceIconUtil() {
        return resourceIconUtil;
    }

    public void setResourceIconUtil(ResourceIconUtil resourceIconUtil) {
        this.resourceIconUtil = resourceIconUtil;
    }

}
