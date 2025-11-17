package org.entando.entando.plugins.jpseo.aps.system.services.linkresolver;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SymbolicLink;
import com.agiletec.plugins.jacms.aps.system.services.contentpagemapper.IContentPageMapperManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.entando.entando.plugins.jpseo.aps.system.services.mapping.ISeoMappingManager;
import org.entando.entando.plugins.jpseo.aps.system.services.url.PageURL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkResolverManagerTest {

    @Mock
    private ISeoMappingManager seoMappingManager;
    @Mock
    private IURLManager urlManager;
    @Mock
    private IContentPageMapperManager contentPageMapperManager;
    @Mock
    private IPageManager pageManager;
    @Mock
    private IAuthorizationManager authorizationManager;
    @Mock
    private IResourceManager resourceManager;
    @Mock
    private RequestContext reqCtx;

    @InjectMocks
    private LinkResolverManager linkResolverManager;

    @Test
    void shouldResolvePageLink() {
        SymbolicLink symbolicLink = new SymbolicLink();
        symbolicLink.setDestType(SymbolicLink.PAGE_TYPE);
        symbolicLink.setPageDestination("pageDestination");
        PageURL pageURL = Mockito.mock(PageURL.class);
        Mockito.when(urlManager.createURL(Mockito.any())).thenReturn(pageURL);
        linkResolverManager.resolveLink(symbolicLink, "NWS3", reqCtx);
        Mockito.verify(pageURL).setPageCode("pageDestination");
    }

    @Test
    void shouldResolveContentOnPageLink() {
        SymbolicLink symbolicLink = new SymbolicLink();
        symbolicLink.setDestType(SymbolicLink.CONTENT_ON_PAGE_TYPE);
        symbolicLink.setContentDestination("NWS1");
        PageURL pageURL = Mockito.mock(PageURL.class);
        Mockito.when(urlManager.createURL(Mockito.any())).thenReturn(pageURL);
        linkResolverManager.resolveLink(symbolicLink, "NWS3", reqCtx);
        Mockito.verify(pageURL).addParam("contentId", "NWS1");
    }

    @Test
    void shouldResolveContentLink() {
        SymbolicLink symbolicLink = new SymbolicLink();
        symbolicLink.setDestType(SymbolicLink.CONTENT_TYPE);
        symbolicLink.setContentDestination("NWS1");
        PageURL pageURL = Mockito.mock(PageURL.class);
        Mockito.when(urlManager.createURL(Mockito.any())).thenReturn(pageURL);
        Mockito.when(contentPageMapperManager.getPageCode("NWS1")).thenReturn("pageCode");
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(reqCtx.getRequest()).thenReturn(servletRequest);
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(servletRequest.getSession()).thenReturn(session);
        User user = new User();
        user.setUsername("admin");
        Mockito.when(session.getAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER)).thenReturn(user);
        IPage page = Mockito.mock(IPage.class);
        Mockito.when(pageManager.getOnlinePage("pageCode")).thenReturn(page);
        Mockito.when(authorizationManager.isAuth(user, page)).thenReturn(true);
        linkResolverManager.resolveLink(symbolicLink, "NWS3", reqCtx);
        Mockito.verify(pageURL).setPageCode("pageCode");
    }

    @Test
    void shouldResolveResourceLink() throws Exception {
        SymbolicLink symbolicLink = new SymbolicLink();
        symbolicLink.setDestType(SymbolicLink.RESOURCE_TYPE);
        symbolicLink.setResourceDestination("resourceDestination");
        ResourceInterface resource = Mockito.mock(ResourceInterface.class);
        Mockito.when(resource.getDefaultUrlPath()).thenReturn("/path/to");
        Mockito.when(resource.getMainGroup()).thenReturn(Group.FREE_GROUP_NAME);
        Mockito.when(resourceManager.loadResource("resourceDestination")).thenReturn(resource);
        String url = linkResolverManager.resolveLink(symbolicLink, "NWS3", reqCtx);
        Assertions.assertEquals("/path/to", url);
    }
}
