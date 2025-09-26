package org.entando.entando.aps.system.services.api.server;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.AuthorizationManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.services.api.ApiCatalogManager;
import org.entando.entando.aps.system.services.api.LegacyApiUnmarshaller;
import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.legacyapi.ApiRestServer;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FlashMapManager;

public abstract class BaseLegacyApiTest extends AbstractControllerTest {

    @Mock
    protected ILangManager langManager;
    @Mock
    protected IURLManager urlManager;
    @Mock
    protected BeanFactory beanFactory;
    @Mock
    protected ApiCatalogManager apiCatalogManager;
    @Mock
    private FlashMapManager flashMapManager;

    @InjectMocks
    protected ResponseBuilder responseBuilder;

    protected ApiRestServer controller;

    @BeforeEach
    public void setUp() throws Exception {
        controller = new ApiRestServer(getUnmarshaller());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();

        WebApplicationContext webApplicationContext = Mockito.mock(WebApplicationContext.class);
        mockMvc.getDispatcherServlet().getServletContext()
                .setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);

        Lang lang = new Lang();
        lang.setCode("en");
        Mockito.when(langManager.getDefaultLang()).thenReturn(lang);

        Mockito.when(urlManager.getApplicationBaseURL(Mockito.any()))
                .thenReturn("http://localhost:8080/entando-de-app");

        Mockito.when(webApplicationContext.getBean(SystemConstants.API_RESPONSE_BUILDER)).thenReturn(responseBuilder);
        Mockito.when(webApplicationContext.getBean(SystemConstants.LANGUAGE_MANAGER)).thenReturn(langManager);
        Mockito.when(webApplicationContext.getBean(SystemConstants.URL_MANAGER)).thenReturn(urlManager);
        Mockito.when(webApplicationContext.getBean(SystemConstants.AUTHORIZATION_SERVICE))
                .thenReturn(authorizationManager);
        Mockito.when(webApplicationContext.getBean("flashMapManager", FlashMapManager.class))
                .thenReturn(flashMapManager);

        Mockito.lenient().when(webApplicationContext.getBean("DefaultObjectMapper"))
                .thenReturn(mapperConfiguration.defaultObjectMapper());
        Mockito.lenient().when(webApplicationContext.getBean("xmlMapper"))
                .thenReturn(mapperConfiguration.xmlMapper());

        responseBuilder.setApiCatalogManager(apiCatalogManager);
        responseBuilder.setBeanFactory(beanFactory);

        Mockito.lenient().when(authorizationManager.getUserRoles(Mockito.any(UserDetails.class))).then(invocation -> {
            UserDetails user = (UserDetails) invocation.getArguments()[0];
            return new AuthorizationManager().getUserRoles(user);
        });
    }

    private LegacyApiUnmarshaller getUnmarshaller() {
        mapperConfiguration.setJsonTypesProviders(getJsonTypesProviders());
        LegacyApiUnmarshaller unmarshaller = new LegacyApiUnmarshaller(
                mapperConfiguration.defaultObjectMapper(), mapperConfiguration.xmlMapper());
        return unmarshaller;
    }

    protected ApiMethod mockApiMethod(ApiMethod.HttpMethod method, String namespace, String resourceName)
            throws Throwable {
        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);
        Mockito.when(apiMethod.getHttpMethod()).thenReturn(method);
        Mockito.lenient().when(apiMethod.getNamespace()).thenReturn(namespace);
        Mockito.lenient().when(apiMethod.getResourceName()).thenReturn(resourceName);
        Mockito.when(apiMethod.isActive()).thenReturn(true);
        Mockito.when(apiCatalogManager.getMethod(method, namespace, resourceName)).thenReturn(apiMethod);
        return apiMethod;
    }
}
