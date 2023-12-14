package org.entando.entando.keycloak.services.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.ConfigTestUtils;
import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.system.services.controller.control.ControlServiceInterface;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * @author E.Santoboni
 */
@ExtendWith(SystemStubsExtension.class)
class KcRequestAuthorizatorTest {
    
    private static ApplicationContext applicationContext;
    private static RequestContext reqCtx;
    
    @SystemStub
    private static EnvironmentVariables environmentVariables;

    private ControlServiceInterface authorizator;

    private IPageManager pageManager;
    
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContextToSet) {
        applicationContext = applicationContextToSet;
    }
    
    @BeforeAll
    public static void startUp() throws Exception {
        environmentVariables.set("KEYCLOAK_ENABLE_GROUPS_IMPORT", "false");
        ServletContext srvCtx = new MockServletContext("", new FileSystemResourceLoader());
        applicationContext = new ConfigTestUtils().createApplicationContext(srvCtx);
        setApplicationContext(applicationContext);
        reqCtx = BaseTestCase.createRequestContext(applicationContext, srvCtx);
        MockHttpServletRequest request = BaseTestCase.createRequest();
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        request.setSession(new MockHttpSession(srvCtx));
        reqCtx.setRequest(request);
        reqCtx.setResponse(new MockHttpServletResponse());
    }
    
    @Test
    void testService_1() throws Throwable {
        ((KcRequestAuthorizator) this.authorizator).setEnabled(false);
        this.executeTestService_1();
        ((KcRequestAuthorizator) this.authorizator).setEnabled(true);
        this.executeTestService_1();
    }

    private void executeTestService_1() throws Throwable {
        this.setUserOnSession(SystemConstants.GUEST_USER_NAME);
        IPage root = this.pageManager.getOnlineRoot();
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, root);
        int status = this.authorizator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.CONTINUE, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertNull(redirectUrl);
    }

    @Test
    void testService_2() throws Throwable {
        this.setUserOnSession("admin");
        IPage root = this.pageManager.getOnlineRoot();
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, root);
        int status = this.authorizator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.CONTINUE, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertNull(redirectUrl);
    }

    @Test
    void testServiceError() throws Throwable {
        int status = this.authorizator.service(reqCtx, ControllerManager.ERROR);
        assertEquals(ControllerManager.ERROR, status);
    }

    @Test
    void testServiceFailure_1() throws Throwable {
        ((MockHttpServletRequest) reqCtx.getRequest()).setRequestURI("/Entando/it/customers_page.page");
        ((MockHttpServletRequest) reqCtx.getRequest()).setQueryString("qsparamname=qsparamvalue");
        this.setUserOnSession(SystemConstants.GUEST_USER_NAME);
        IPage requiredPage = this.pageManager.getOnlinePage("customers_page");
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, requiredPage);
        int status = this.authorizator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.REDIRECT, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertTrue(redirectUrl.contains("/Entando/do/login?"));
        assertTrue(redirectUrl.contains("redirectflag"));
        assertTrue(redirectUrl.contains("redirectTo="));
        assertTrue(redirectUrl.contains("customers_page.page"));
        assertTrue(redirectUrl.contains("qsparamname"));
        assertTrue(redirectUrl.contains("qsparamvalue"));
    }

    @Test
    void testServiceFailure_2() throws Throwable {
        reqCtx.getRequest().getSession().removeAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);
        IPage root = this.pageManager.getOnlineRoot();
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, root);
        int status = this.authorizator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.SYS_ERROR, status);
    }

    @Test
    void testUserNotAuthorized() throws Throwable {
        ((MockHttpServletRequest) reqCtx.getRequest()).setRequestURI("/Entando/it/coach_page.page");
        this.setUserOnSession("editorCustomers");
        IPage requiredPage = this.pageManager.getOnlinePage("coach_page");
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, requiredPage);
        int status = this.authorizator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.CONTINUE, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertNull(redirectUrl);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, reqCtx.getResponse().getStatus());
        IPage currentPage = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        assertEquals("errorpage", currentPage.getCode());
    }

    @BeforeEach
    void init() throws Exception {
        try {
            this.authorizator = (ControlServiceInterface) applicationContext.getBean("RequestAuthorizatorControlService");
            this.pageManager = (IPageManager) applicationContext.getBean(SystemConstants.PAGE_MANAGER);
            reqCtx.removeExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
            ((KcRequestAuthorizator) this.authorizator).setEnabled(true);
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }
    
    public static void setUserOnSession(String username) throws Exception {
        UserDetails currentUser = getUser(username);
        HttpSession session = reqCtx.getRequest().getSession();
        session.setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, currentUser);
    }
    
    protected static UserDetails getUser(String username) throws Exception {
        IAuthenticationProviderManager provider = 
                (IAuthenticationProviderManager) applicationContext.getBean(SystemConstants.AUTHENTICATION_PROVIDER_MANAGER);
        IUserManager userManager = applicationContext.getBean(SystemConstants.USER_MANAGER, IUserManager.class);
        UserDetails user = null;
        if (username.equals(SystemConstants.GUEST_USER_NAME)) {
            user = userManager.getGuestUser();
        } else {
            user = provider.getUser(username, username);
        }
        return user;
    }

}
