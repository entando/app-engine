package org.entando.entando.plugins.jpsolr.aps.system.content.widget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.entando.entando.plugins.jpsolr.aps.system.content.IAdvContentFacetManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class FacetNavHelperTest {

    @Mock
    private IAdvContentFacetManager facetManager;
    @Mock
    private RequestContext reqCtx;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpSession session;
    @Mock
    private ServletContext servletContext;
    @Mock
    private WebApplicationContext webApplicationContext;
    @Mock
    private IContentManager contentManager;
    @Mock
    private IAuthorizationManager authManager;

    @InjectMocks
    private FacetNavHelper helper;

    @Test
    void shouldGetResult() throws Exception {
        Widget currentWidget = new Widget();
        ApsProperties config = new ApsProperties();
        config.setProperty(JpSolrSystemConstants.CONTENT_TYPES_FILTER_WIDGET_PARAM_NAME, "NWS");
        currentWidget.setConfig(config);
        Mockito.when(reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET)).thenReturn(currentWidget);

        Group group = new Group();
        group.setName("group1");
        Mockito.when(authManager.getUserGroups(any())).thenReturn(List.of(group));

        Mockito.when(reqCtx.getRequest()).thenReturn(servletRequest);
        Mockito.when(servletRequest.getSession()).thenReturn(session);
        Mockito.when(session.getServletContext()).thenReturn(servletContext);
        Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webApplicationContext);

        Mockito.when(webApplicationContext.getBean(JacmsSystemConstants.CONTENT_MANAGER)).thenReturn(contentManager);
        Mockito.when(webApplicationContext.getBean(SystemConstants.AUTHORIZATION_SERVICE)).thenReturn(authManager);

        SmallEntityType entityTypeNws = new SmallEntityType();
        entityTypeNws.setCode("NWS");
        SmallEntityType entityTypeBnr = new SmallEntityType();
        entityTypeBnr.setCode("BNR");
        Mockito.when(contentManager.getSmallEntityTypes()).thenReturn(List.of(entityTypeNws, entityTypeBnr));

        helper.getResult(List.of("category1", "category2"), reqCtx);

        ArgumentCaptor<SearchEngineFilter[]> filtersCaptor = ArgumentCaptor.forClass(SearchEngineFilter[].class);
        ArgumentCaptor<List<String>> groupsCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(facetManager).getFacetResult(
                filtersCaptor.capture(), any(List.class), isNull(), groupsCaptor.capture());

        SearchEngineFilter[] filters = filtersCaptor.getValue();
        Assertions.assertEquals(2, filters.length);
        Assertions.assertEquals("NWS", filters[0].getAllowedValues().get(0));

        List<String> groups = groupsCaptor.getValue();
        Assertions.assertEquals(2, groups.size());
        Assertions.assertEquals(Group.FREE_GROUP_NAME, groups.get(0));
        Assertions.assertEquals("group1", groups.get(1));
    }
}
