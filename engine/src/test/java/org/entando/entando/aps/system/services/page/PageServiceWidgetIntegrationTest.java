package org.entando.entando.aps.system.services.page;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.system.services.page.PageTestUtil;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.aps.system.services.page.model.PageConfigurationDto;
import org.entando.entando.aps.system.services.page.model.WidgetConfigurationDto;
import org.entando.entando.web.page.model.WidgetConfigurationRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageServiceWidgetIntegrationTest extends BaseTestCase {

    private IPageService pageService;
    private IPageManager pageManager;
    private IPageModelManager pageModelManager;
    
    @BeforeEach
    private void init() throws Exception {
        try {
            pageService = (IPageService) this.getApplicationContext().getBean(IPageService.BEAN_NAME);
            pageManager = (IPageManager) this.getApplicationContext().getBean(SystemConstants.PAGE_MANAGER);
            pageModelManager = (IPageModelManager) this.getApplicationContext().getBean(SystemConstants.PAGE_MODEL_MANAGER);
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    void testGetPageConfiguration() throws JsonProcessingException {
        IPage draftRoot = this.pageManager.getDraftRoot();
        PageConfigurationDto pageConfigurationDto = (PageConfigurationDto) this.pageService.getPageConfiguration(draftRoot.getCode(), IPageService.STATUS_DRAFT);
        ObjectMapper mapper = new ObjectMapper();
        String out = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pageConfigurationDto);
        Assertions.assertNotNull(out);
    }

    @Test
    void testUpdatePageWidget() throws JsonProcessingException, EntException {
        String pageCode = "temp001";
        IPage parentPage = pageManager.getDraftRoot();
        PageModel pageModel = this.pageModelManager.getPageModel(parentPage.getMetadata().getModelCode());
        PageMetadata metadata = PageTestUtil.createPageMetadata(pageModel,
                                                                true, pageCode, null, null, false, null, null);
        Page pageToAdd = PageTestUtil.createPage(pageCode, parentPage.getCode(), "free", pageModel, metadata, null);
        try {
            pageManager.addPage(pageToAdd);
            WidgetConfigurationDto widgetConfigurationDto = this.pageService.getWidgetConfiguration(pageToAdd.getCode(), 0, IPageService.STATUS_DRAFT);
            assertThat(widgetConfigurationDto, is(nullValue()));

            WidgetConfigurationRequest widgetConfigurationRequest = new WidgetConfigurationRequest();
            widgetConfigurationRequest.setCode("login_form");
            widgetConfigurationRequest.setConfig(null);

            this.pageService.updateWidgetConfiguration(pageCode, 0, widgetConfigurationRequest);

            assertThat(this.pageService.getWidgetConfiguration(pageToAdd.getCode(), 0, IPageService.STATUS_DRAFT).getCode(), is("login_form"));

        } finally {
            pageManager.deletePage(pageCode);
        }
    }

    @Test
    void testRemovePageWidget() throws JsonProcessingException, EntException {
        String pageCode = "temp001";
        IPage parentPage = pageManager.getDraftRoot();
        PageModel pageModel = this.pageModelManager.getPageModel(parentPage.getMetadata().getModelCode());
        PageMetadata metadata = PageTestUtil.createPageMetadata(pageModel,
                                                                true, pageCode, null, null, false, null, null);
        Page pageToAdd = PageTestUtil.createPage(pageCode, parentPage.getCode(), "free", pageModel, metadata, null);
        try {
            pageManager.addPage(pageToAdd);
            WidgetConfigurationDto widgetConfigurationDto = this.pageService.getWidgetConfiguration(pageToAdd.getCode(), 0, IPageService.STATUS_DRAFT);
            assertThat(widgetConfigurationDto, is(nullValue()));

            WidgetConfigurationRequest widgetConfigurationRequest = new WidgetConfigurationRequest();
            widgetConfigurationRequest.setCode("login_form");
            widgetConfigurationRequest.setConfig(null);

            this.pageService.updateWidgetConfiguration(pageCode, 0, widgetConfigurationRequest);
            assertThat(this.pageService.getWidgetConfiguration(pageToAdd.getCode(), 0, IPageService.STATUS_DRAFT).getCode(), is("login_form"));

            this.pageService.deleteWidgetConfiguration(pageToAdd.getCode(), 0);
            assertThat(widgetConfigurationDto, is(nullValue()));

        } finally {
            pageManager.deletePage(pageCode);
        }
    }


}
