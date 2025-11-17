package org.entando.entando.aps.system.services.api.server;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import com.agiletec.aps.system.services.user.UserDetails;
import java.util.List;
import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.aps.system.services.api.model.LinkedListApiResponse;
import org.entando.entando.aps.system.services.api.model.LinkedListItem;
import org.entando.entando.aps.system.services.api.model.StringApiResponse;
import org.entando.entando.aps.system.services.guifragment.api.ApiGuiFragmentInterface;
import org.entando.entando.aps.system.services.guifragment.api.GuiFragmentResponse;
import org.entando.entando.aps.system.services.guifragment.api.JAXBGuiFragment;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ApiRestServerGuiFragmentTest extends BaseLegacyApiTest {

    @Mock
    private ApiGuiFragmentInterface apiGuiFragmentInterface;

    private String accessToken;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.when(beanFactory.getBean("ApiGuiFragmentInterface")).thenReturn(apiGuiFragmentInterface);
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        accessToken = mockOAuthInterceptor(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragments", "/guiFragments.json"})
    void shouldGetGuiFragmentsJson(String path) throws Throwable {
        mockGetGuiFragments();

        ResultActions result = mockMvc
                .perform(get("/legacy/en/core" + path)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(jsonPath("result.items.size", is("1")))
                .andExpect(jsonPath("result.items.item[0].code", is("breadcrumb")))
                .andExpect(jsonPath("result.items.item[0].url",
                        is("http://localhost:8080/entando-de-app/api/legacy/en/core/guiFragment?code=breadcrumb")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragments", "/guiFragments.xml"})
    void shouldGetGuiFragmentsXml(String path) throws Throwable {
        mockGetGuiFragments();

        ResultActions result = mockMvc
                .perform(get("/legacy/en/core" + path)
                        .accept(MediaType.APPLICATION_XML)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(xpath("/response/result/items/size").string("1"))
                .andExpect(xpath("/response/result/items/item[1]/code").string("breadcrumb"))
                .andExpect(xpath("/response/result/items/item[1]/url")
                        .string("http://localhost:8080/entando-de-app/api/legacy/en/core/guiFragment?code=breadcrumb"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.json"})
    void shouldGetGuiFragmentJson(String path) throws Throwable {
        mockGetGuiFragment();

        ResultActions result = mockMvc
                .perform(get("/legacy/en/core" + path)
                        .param("code", "fragmentCode")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(jsonPath("result.guiFragment.code", is("fragmentCode")))
                .andExpect(jsonPath("result.guiFragment.widgetTypeCode", is("fragmentCode")))
                .andExpect(jsonPath("result.guiFragment.gui", is("<p>hello</p>")))
                .andExpect(jsonPath("result.guiFragment.locked", is(false)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.xml"})
    void shouldGetGuiFragmentXml(String path) throws Throwable {
        mockGetGuiFragment();

        ResultActions result = mockMvc
                .perform(get("/legacy/en/core" + path)
                        .param("code", "fragmentCode")
                        .accept(MediaType.APPLICATION_XML)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(xpath("/response/result/guiFragment/code").string("fragmentCode"))
                .andExpect(xpath("/response/result/guiFragment/widgetTypeCode").string("fragmentCode"))
                .andExpect(xpath("/response/result/guiFragment/gui").string("<p>hello</p>"))
                .andExpect(xpath("/response/result/guiFragment/locked").booleanValue(false))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.json"})
    void shouldPostGuiFragmentJson(String path) throws Throwable {
        mockPostGuiFragment();

        String requestPayload = "{\n"
                + "    \"code\": \"myfragment\",\n"
                + "    \"pluginCode\": null,\n"
                + "    \"gui\": \"<p>hello</p>\",\n"
                + "    \"defaultGui\": null,\n"
                + "    \"locked\": false\n"
                + "}";

        ResultActions result = mockMvc
                .perform(post("/legacy/en/core" + path)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestPayload)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(jsonPath("result", is("SUCCESS")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.xml"})
    void shouldPostGuiFragmentXml(String path) throws Throwable {
        mockPostGuiFragment();

        String requestPayload = "<guiFragment>\n"
                + "    <code>myfragment</code>\n"
                + "    <gui>&lt;p&gt;hello&lt;/p&gt;</gui>\n"
                + "    <locked>false</locked>\n"
                + "</guiFragment>";

        ResultActions result = mockMvc
                .perform(post("/legacy/en/core" + path)
                        .accept(MediaType.APPLICATION_XML)
                        .contentType(MediaType.APPLICATION_XML)
                        .content(requestPayload)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(xpath("/response/result").string("SUCCESS"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.json"})
    void shouldPutGuiFragmentJson(String path) throws Throwable {
        mockPutGuiFragment();

        String requestPayload = "{\n"
                + "    \"code\": \"myfragment\",\n"
                + "    \"pluginCode\": null,\n"
                + "    \"gui\": \"<p>hello</p>\",\n"
                + "    \"defaultGui\": null,\n"
                + "    \"locked\": false\n"
                + "}";

        ResultActions result = mockMvc
                .perform(put("/legacy/en/core" + path)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestPayload)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(jsonPath("result", is("SUCCESS")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.xml"})
    void shouldPutGuiFragmentXml(String path) throws Throwable {
        mockPutGuiFragment();

        String requestPayload = "<guiFragment>\n"
                + "    <code>myfragment</code>\n"
                + "    <gui>&lt;p&gt;hello&lt;/p&gt;</gui>\n"
                + "    <locked>false</locked>\n"
                + "</guiFragment>";

        ResultActions result = mockMvc
                .perform(put("/legacy/en/core" + path)
                        .accept(MediaType.APPLICATION_XML)
                        .contentType(MediaType.APPLICATION_XML)
                        .content(requestPayload)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(xpath("/response/result").string("SUCCESS"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.json"})
    void shouldDeleteGuiFragmentJson(String path) throws Throwable {
        mockDeleteGuiFragment();

        ResultActions result = mockMvc
                .perform(delete("/legacy/en/core" + path)
                        .param("code", "fragmentCode")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(jsonPath("result", is("SUCCESS")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/guiFragment", "/guiFragment.xml"})
    void shouldDeleteGuiFragmentXml(String path) throws Throwable {
        mockDeleteGuiFragment();

        ResultActions result = mockMvc
                .perform(delete("/legacy/en/core" + path)
                        .param("code", "fragmentCode")
                        .accept(MediaType.APPLICATION_XML)
                        .header("Authorization", "Bearer " + accessToken));
        result
                .andExpect(xpath("/response/result").string("SUCCESS"))
                .andExpect(status().isOk());
    }

    private void mockGetGuiFragments() throws Throwable {
        ApiMethod apiMethod = mockApiMethod(ApiMethod.HttpMethod.GET, "core", "guiFragments");
        Mockito.when(apiMethod.getSpringBeanMethod()).thenReturn("getGuiFragments");
        Mockito.when(apiMethod.getResponseClassName()).thenReturn(LinkedListApiResponse.class.getCanonicalName());

        LinkedListItem breadcrumbFragment = new LinkedListItem();
        breadcrumbFragment.setCode("breadcrumb");
        breadcrumbFragment.setUrl(
                "http://localhost:8080/entando-de-app/api/legacy/en/core/guiFragment?code=breadcrumb");
        List<LinkedListItem> fragments = List.of(breadcrumbFragment);
        Mockito.when(apiGuiFragmentInterface.getGuiFragments(Mockito.any())).thenReturn(fragments);
    }

    private void mockGetGuiFragment() throws Throwable {
        ApiMethod apiMethod = mockApiMethod(ApiMethod.HttpMethod.GET, "core", "guiFragment");
        Mockito.when(apiMethod.getSpringBeanMethod()).thenReturn("getGuiFragment");
        Mockito.when(apiMethod.getResponseClassName()).thenReturn(GuiFragmentResponse.class.getCanonicalName());

        JAXBGuiFragment guiFragment = new JAXBGuiFragment();
        guiFragment.setCode("fragmentCode");
        guiFragment.setWidgetTypeCode("fragmentCode");
        guiFragment.setGui("<p>hello</p>");
        Mockito.when(apiGuiFragmentInterface.getGuiFragment(Mockito.any())).thenReturn(guiFragment);
    }

    private void mockPostGuiFragment() throws Throwable {
        ApiMethod apiMethod = mockApiMethod(ApiMethod.HttpMethod.POST, "core", "guiFragment");
        Mockito.when(apiMethod.getSpringBeanMethod()).thenReturn("addGuiFragment");
        Mockito.when(apiMethod.getExpectedType()).thenReturn(JAXBGuiFragment.class);
    }

    private void mockPutGuiFragment() throws Throwable {
        ApiMethod apiMethod = mockApiMethod(ApiMethod.HttpMethod.PUT, "core", "guiFragment");
        Mockito.when(apiMethod.getSpringBeanMethod()).thenReturn("updateGuiFragment");
        Mockito.when(apiMethod.getExpectedType()).thenReturn(JAXBGuiFragment.class);
    }

    private void mockDeleteGuiFragment() throws Throwable {
        ApiMethod apiMethod = mockApiMethod(ApiMethod.HttpMethod.DELETE, "core", "guiFragment");
        Mockito.when(apiMethod.getSpringBeanMethod()).thenReturn("deleteGuiFragment");
    }

    @Override
    protected ApiMethod mockApiMethod(ApiMethod.HttpMethod method, String namespace, String resourceName)
            throws Throwable {
        ApiMethod apiMethod = super.mockApiMethod(method, namespace, resourceName);
        Mockito.when(apiMethod.getSpringBean()).thenReturn("ApiGuiFragmentInterface");
        return apiMethod;
    }
}
