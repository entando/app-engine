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
package it.cte.aps.system.services.Page;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.AbstractParameterizableService;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupUtilizer;
import com.agiletec.aps.system.services.lang.events.LangsChangedEvent;
import com.agiletec.aps.system.services.lang.events.LangsChangedObserver;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageDAO;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.PageManager;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.system.services.page.PagesStatus;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.page.cache.IPageManagerCacheWrapper;
import com.agiletec.aps.system.services.page.events.PageChangedEvent;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.system.services.pagemodel.PageModelUtilizer;
import com.agiletec.aps.system.services.pagemodel.events.PageModelChangedEvent;
import com.agiletec.aps.system.services.pagemodel.events.PageModelChangedObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.aps.system.services.widgettype.events.WidgetTypeChangedEvent;
import org.entando.entando.aps.system.services.widgettype.events.WidgetTypeChangedObserver;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * This is the page manager service class. Pages are held in a tree-like structure, to allow a hierarchical access, and
 * stored in a map, to allow a key-value type access. In the tree, the father points the son and vice versa; the order
 * between the pages in the same level is always kept.
 *
 * @author M.Diana - E.Santoboni
 */
public class CtePageManager extends PageManager implements IPageManager, GroupUtilizer<IPage>,
        LangsChangedObserver, PageModelUtilizer, PageModelChangedObserver, WidgetTypeChangedObserver,
        RefreshableBeanTenantAware {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(CtePageManager.class);
    public static final String ERRMSG_ERROR_WHILE_MOVING_A_PAGE = "Error while moving a page";

    @Autowired
    @Qualifier(value = "PageManagerParameterNames")
    public transient List<String> parameterNames;

    private transient IPageManagerCacheWrapper cacheWrapper;
    private transient IPageModelManager pageModelManager;
    private transient IPageDAO pageDao;

    @Override
    public void init() throws Exception {
        initTenantAware();
        _logger.debug("{} ready. : Initialized", this.getClass().getName());
    }


    /**
     * @param pageCode
     * @param widget
     * @param pos
     * @throws EntException In case of error.
     * @deprecated Use {@link #joinWidget(String,Widget,int)} instead
     */
    @Override
    @Deprecated
    public synchronized void joinShowlet(String pageCode, Widget widget, int pos) throws EntException {
        this.joinWidget(pageCode, widget, pos);
    }

    /**
     * Set the widget -including its configuration- in the given page in the
     * desired position. If the position is already occupied by another widget
     * this will be substituted with the new one.
     *
     * @param pageCode the code of the page where to set the widget
     * @param widget The widget to set
     * @param pos The position where to place the widget in
     * @throws EntException In case of error.
     */
    @Override
    public synchronized void joinWidget(String pageCode, Widget widget, int pos) throws EntException {
        this.checkPagePos(pageCode, pos);

        if (null == widget || null == widget.getTypeCode()) {
            throw new EntException("Invalid null value found in either the Widget or the widgetType");
        }

        System.out.println("\n>>>\n>>> " + widget.getTypeCode() + "\n>>>\n");

        try {
            IPage currentPage = this.getDraftPage(pageCode);
            this.getPageDAO().joinWidget(currentPage, widget, pos);
            currentPage.getWidgets()[pos] = widget;
            if (currentPage.isOnline()) {
                boolean widgetEquals = Arrays
                        .deepEquals(currentPage.getWidgets(), this.getOnlinePage(pageCode).getWidgets());
                ((Page) currentPage).setChanged(!widgetEquals);
            }
            this.getCacheWrapper().updateDraftPage(currentPage);
            this.notifyPageChangedEvent(currentPage, PageChangedEvent.EDIT_FRAME_OPERATION_CODE, pos,
                    PageChangedEvent.EVENT_TYPE_JOIN_WIDGET);
        } catch (Exception e) {
            String message = "Error during the assignation of a widget to the frame " + pos + " in the page code " + pageCode;
            _logger.error("Error during the assignation of a widget to the frame {} in the page code {}", pos, pageCode, e);
            throw new EntException(message, e);
        }
    }

    /**
     * Utility method which perform checks on the parameters submitted when
     * editing the page.
     *
     * @param pageCode The code of the page
     * @param pos The given position
     * @throws EntException In case of database access error.
     */
    private void checkPagePos(String pageCode, int pos) throws EntException {
        IPage currentPage = this.getDraftPage(pageCode);
        if (null == currentPage) {
            throw new EntException("The page '" + pageCode + "' does not exist!");
        }
        PageMetadata metadata = currentPage.getMetadata();
        if (null == metadata) {
            throw new EntException("Null metadata for page '" + pageCode + "'!");
        }
        PageModel model = this.getPageModelManager().getPageModel(metadata.getModelCode());
        if (pos < 0 || pos >= model.getFrames().length) {
            throw new EntException("The Position '" + pos + "' is not defined in the model '" + model.getDescription() + "' of the page '" + pageCode + "'!");
        }
    }


    private void notifyPageChangedEvent(IPage page, int operationCode, Integer framesPos, Integer destFramePos, String eventType) {
        PageChangedEvent event = buildEvent(page, operationCode, framesPos);
        Map<String, String> properties = new HashMap<>();
        Optional.ofNullable(page).ifPresent(p -> properties.put("pageCode", p.getCode()));
        properties.put("operationCode", String.valueOf(operationCode));
        Optional.ofNullable(framesPos).ifPresent(p -> properties.put("framesPos", String.valueOf(p)));
        Optional.ofNullable(destFramePos).ifPresent(p -> {
            properties.put("destFramePos", String.valueOf(p));
            event.setDestFrame(p);
        });
        Optional.ofNullable(eventType).ifPresent(p -> {
            properties.put("eventType", p);
            event.setEventType(p);
        });
        event.setMessage(properties);
        event.setChannel(SystemConstants.PAGE_EVENT_CHANNEL);
        this.notifyEvent(event);
    }

    private void notifyPageChangedEvent(IPage page, int operationCode, Integer framePos, String eventType) {
        this.notifyPageChangedEvent(page, operationCode, framePos, null, eventType);
    }

    private PageChangedEvent buildEvent(IPage page, int operationCode, Integer framePos) {
        PageChangedEvent event = new PageChangedEvent();
        event.setPage(page);
        event.setOperationCode(operationCode);
        if (null != framePos) {
            event.setFramePosition(framePos);
        }
        return event;
    }

}
