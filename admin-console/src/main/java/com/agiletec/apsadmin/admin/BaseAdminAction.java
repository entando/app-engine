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
package com.agiletec.apsadmin.admin;

import static com.agiletec.aps.system.services.reload.ReloadConfigThread.RELOAD_THREAD;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.event.ReloadingEntitiesReferencesEvent;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.baseconfig.SystemParamsUtils;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.reload.ReloadConfigThread;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.apsadmin.system.BaseAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.web.context.WebApplicationContext;

/**
 * This base action implements the default actions available for the system
 * administration.
 *
 * @author E.Santoboni
 */
public class BaseAdminAction extends BaseAction {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(BaseAdminAction.class);

    public static final String RELOAD_ERROR = "reloadError";
    public static final String IN_PROGRESS = "inProgress";
    public static final String NEW_PARAM_MARKER = "_newParamMarker";

    /**
     * Reload the system configuration.
     *
     * @return the result code.
     */
    public String reloadConfig() {
        try {
            if (!ApsWebApplicationUtils.isReloadInProgress()) {
                ReloadConfigThread rct = new ReloadConfigThread(this.getRequest());
                logger.info("Starting reload configuration thread");
                rct.start();
            } else {
                logger.info("Reload operation already in progress!");
            }
            this.setReloadingResult(PROGRESS_RELOADING_RESULT_CODE);
        } catch (Exception e) {
            logger.error("unexpected error while launching system reload", e);
            this.setReloadingResult(FAILURE_RELOADING_RESULT_CODE);
            return RELOAD_ERROR;
        }
        return SUCCESS;
    }

    public String reloadStatus() {
        if (ApsWebApplicationUtils.isReloadInProgress()) {
            this.setReloadingResult(PROGRESS_RELOADING_RESULT_CODE);
            return IN_PROGRESS;
        }
        updateReloadStatusResult();
        return SUCCESS;
    }

    public String reloadStatusJson() {
        return SUCCESS;
    }

    public boolean isReloadingErrorDetect() {
        return ApsWebApplicationUtils.getReloadInfo()
                .values()
                .stream()
                .anyMatch(StringUtils::isNotBlank);
    }

    public int getReloadProgress() {
        return ApsWebApplicationUtils.getReloadProgress();
    }

    public Map<String, String> getReloadInfo() {
        return new HashMap<>(ApsWebApplicationUtils.getReloadInfo());
    }

    /**
     * Reload the references of all the existing entities.
     *
     * @return the result code.
     */
    public String reloadEntitiesReferences() {
        try {
            ReloadingEntitiesReferencesEvent event = new ReloadingEntitiesReferencesEvent();
            WebApplicationContext wac = ApsWebApplicationUtils.getWebApplicationContext(this.getRequest());
            wac.publishEvent(event);
            logger.info("Reloading entity references started");
        } catch (Throwable t) {
            logger.error("error in reloadEntitiesReferences", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    public int getReloadingResult() {
        return _reloadingResult;
    }

    public void setReloadingResult(int reloadingResult) {
        this._reloadingResult = reloadingResult;
    }

    /**
     * Get the system parameters to edit them.
     *
     * @return the result code.
     */
    public String configSystemParams() {
        try {
            this.initLocalMap();
        } catch (Throwable t) {
            logger.error("error in configSystemParams", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Update the system params.
     *
     * @return the result code.
     */
    public String updateSystemParams() {
        return this.updateSystemParams(false);
    }

    public String updateSystemParamsForAjax() {
        return this.updateSystemParams(true);
    }

    protected String updateSystemParams(boolean keepOldParam) {
        try {
            this.initLocalMap();
            this.updateLocalParams(keepOldParam);
            this.extractExtraParameters();
            String xmlParams = this.getConfigParameter();
            String newXmlParams = SystemParamsUtils.getNewXmlParams(xmlParams, this.getSystemParams());
            this.getConfigManager().updateConfigItem(SystemConstants.CONFIG_ITEM_PARAMS, newXmlParams);
            this.addActionMessage(this.getText("message.configSystemParams.ok"));
        } catch (Throwable t) {
            logger.error("error in updateSystemParams", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    protected void initLocalMap() throws Throwable {
        String xmlParams = this.getConfigParameter();
        Map<String, String> systemParams = SystemParamsUtils.getParams(xmlParams);
        this.setSystemParams(systemParams);
    }

    protected String getConfigParameter() {
        return this.getConfigManager().getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
    }

    /**
     * Refresh the map of parameters with values fetched from the request
     *
     * @param keepOldParam when true, when a system parameter is not found in
     *  the request, the previous system parameter will be stored
     */
    protected void updateLocalParams(boolean keepOldParam) {
        Iterator<String> paramNames = this.getSystemParams().keySet().iterator();
        while (paramNames.hasNext()) {
            String paramName = (String) paramNames.next();
            String newValue = this.getRequest().getParameter(paramName);
            if (null != newValue) {
                this.getSystemParams().put(paramName, newValue);
            } else if (!keepOldParam) {
                this.getSystemParams().put(paramName, "false");
            }
        }
    }

    public void extractExtraParameters() {
        try {
            Enumeration<String> parameterNames = this.getRequest().getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                if (paramName.endsWith(this.getExternalParamMarker())) {
                    String newParamName = paramName.substring(0, paramName.indexOf(this.getExternalParamMarker()));
                    if (null == this.getSystemParams().get(newParamName)) {
                        String newParamValue = this.getRequest().getParameter(newParamName);
                        if (null != newParamValue) {
                            this.getSystemParams().put(newParamName, newParamValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting extra parameters", e);
        }
    }

    /**
     * Return a plain list of the free pages in the portal.
     *
     * @return the list of the free pages of the portal.
     */
    public List<IPage> getFreePages() {
        IPage root = this.getPageManager().getOnlineRoot();
        List<IPage> pages = new ArrayList<>();
        this.addFreePublicPages(root, pages);
        return pages;
    }

    private void addFreePublicPages(IPage page, List<IPage> pages) {
        if (null == page) {
            return;
        }
        if (page.getGroup().equals(Group.FREE_GROUP_NAME)) {
            pages.add(page);
        }
        String[] children = page.getChildrenCodes();
        for (int i = 0; i < children.length; i++) {
            IPage child = this.getPageManager().getOnlinePage(children[i]);
            this.addFreePublicPages(child, pages);
        }
    }

    protected void updateReloadStatusResult() {
        if (ApsWebApplicationUtils.getReloadInfo().containsKey(RELOAD_THREAD)) {
            this.setReloadingResult(FAILURE_RELOADING_RESULT_CODE);
        } else if (isReloadingErrorDetect()) {
            this.setReloadingResult(WARNING_RELOADING_RESULT_CODE);
        } else {
            this.setReloadingResult(SUCCESS_RELOADING_RESULT_CODE);
        }
    }

    protected ConfigInterface getConfigManager() {
        return _configManager;
    }

    public void setConfigManager(ConfigInterface configManager) {
        this._configManager = configManager;
    }

    protected IPageManager getPageManager() {
        return _pageManager;
    }

    public void setPageManager(IPageManager pageManager) {
        this._pageManager = pageManager;
    }

    public Map<String, String> getSystemParams() {
        return _systemParams;
    }

    public void setSystemParams(Map<String, String> systemParams) {
        this._systemParams = systemParams;
    }

    public String getExternalParamMarker() {
        return NEW_PARAM_MARKER;
    }

    private ConfigInterface _configManager;
    private IPageManager _pageManager;

    private Map<String, String> _systemParams;

    private int _reloadingResult = -1;

    public static final int FAILURE_RELOADING_RESULT_CODE = 0;
    public static final int SUCCESS_RELOADING_RESULT_CODE = 1;
    public static final int PROGRESS_RELOADING_RESULT_CODE = 2;
    public static final int WARNING_RELOADING_RESULT_CODE = 3;

}
