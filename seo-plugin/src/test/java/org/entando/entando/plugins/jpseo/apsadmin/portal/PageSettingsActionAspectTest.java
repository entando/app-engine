/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpseo.apsadmin.portal;

import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.apsadmin.portal.PageSettingsAction;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.util.ValueStack;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import org.apache.struts2.ServletActionContext;
import org.aspectj.lang.JoinPoint;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.plugins.jpseo.aps.system.JpseoSystemConstants;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.page.IPageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class PageSettingsActionAspectTest {

    private static final String CONFIG_PARAMETER
            = "<Params><Param name=\"param_1\">value_1</Param><Param name=\"param_2\">value_2</Param></Params>";

    private MockHttpServletRequest request = new MockHttpServletRequest();

    @Mock
    private IPageManager pageManager;

    @Mock
    private IStorageManager storageManager;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private TextProvider textProvider;

    @InjectMocks
    private PageSettingsAction pageSettingsAction;

    @InjectMocks
    private PageSettingsActionAspect actionAspect;

    @BeforeEach
    public void setUp() throws Exception {
        ActionContext actionContext = Mockito.mock(ActionContext.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        request.setSession(new MockHttpSession(servletContext));
        Mockito.lenient().when(actionContext.getLocale()).thenReturn(Locale.ENGLISH);
        ValueStack valueStack = Mockito.mock(ValueStack.class);
        Map<String, Object> context = new HashMap<>();
        Container container = Mockito.mock(Container.class);
        XWorkConverter conv = Mockito.mock(XWorkConverter.class);
        Mockito.lenient().when(container.getInstance(XWorkConverter.class)).thenReturn(conv);
        Mockito.lenient().when(conv.convertValue(Mockito.any(Map.class), Mockito.any(Object.class), Mockito.any(Class.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return "VALUE";
            }
        });
        context.put(ActionContext.CONTAINER, container);
        Mockito.lenient().when(valueStack.getContext()).thenReturn(context);
        Mockito.lenient().when(actionContext.getValueStack()).thenReturn(valueStack);
        when(actionContext.get(ServletActionContext.HTTP_REQUEST)).thenReturn(request);
        ServletActionContext.setContext(actionContext);
        when(joinPoint.getTarget()).thenReturn(pageSettingsAction);
        Mockito.lenient().when(textProvider.getText(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn("text");
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.request.removeAllParameters();
        String path = System.getProperty("java.io.tmpdir") + File.separator + "robot.txt";
        File tempFile = new File(path);
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void executeInitConfig_1() throws EntException {
        String path = System.getProperty("java.io.tmpdir") + File.separator + "robot.txt";
        when(pageManager.getConfig(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME)).thenReturn(path);
        actionAspect.executeInitConfig(joinPoint);
        Mockito.verify(storageManager, Mockito.times(0)).exists(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertTrue(pageSettingsAction.hasFieldErrors());
        Assertions.assertEquals(1, pageSettingsAction.getFieldErrors().get(PageSettingsActionAspect.PARAM_ROBOT_CONTENT_CODE).size());
    }

    @Test
    public void executeInitConfig_2() throws EntException {
        String path = System.getProperty("java.io.tmpdir") + File.separator + "meta-inf" + File.separator + "robot.txt";
        when(pageManager.getConfig(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME)).thenReturn(path);
        actionAspect.executeInitConfig(joinPoint);
        Mockito.verify(storageManager, Mockito.times(0)).exists(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertTrue(pageSettingsAction.hasFieldErrors());
        Assertions.assertEquals(1, pageSettingsAction.getFieldErrors().get(PageSettingsActionAspect.PARAM_ROBOT_ALTERNATIVE_PATH_CODE).size());
    }

    @Test
    public void executeInitConfig_3() throws EntException {
        this.request.getSession().setAttribute(PageSettingsActionAspect.SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE_ERROR, "Message");
        String path = System.getProperty("java.io.tmpdir") + File.separator + "robot.txt";
        when(pageManager.getConfig(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME)).thenReturn(path);
        actionAspect.executeInitConfig(joinPoint);
        Mockito.verify(storageManager, Mockito.times(0)).exists(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertTrue(pageSettingsAction.hasFieldErrors());
        Assertions.assertEquals(1, pageSettingsAction.getFieldErrors().get(PageSettingsActionAspect.PARAM_ROBOT_ALTERNATIVE_PATH_CODE).size());
        Assertions.assertEquals("Message", pageSettingsAction.getFieldErrors().get(PageSettingsActionAspect.PARAM_ROBOT_ALTERNATIVE_PATH_CODE).get(0));
    }

    @Test
    public void executeUpdateSystemParams_1() throws Exception {
        actionAspect.executeUpdateSystemParams(joinPoint);
        Mockito.verify(storageManager, Mockito.times(0)).saveFile(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(InputStream.class));
        Mockito.verify(storageManager, Mockito.times(1)).deleteFile(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertFalse(pageSettingsAction.hasFieldErrors());
        Mockito.verify(pageManager, Mockito.times(1)).updateParams(Mockito.any());
    }

    @Test
    public void executeUpdateSystemParams_2() throws Exception {
        this.request.setParameter(PageSettingsActionAspect.PARAM_ROBOT_CONTENT_CODE, "Robot content");
        actionAspect.executeUpdateSystemParams(joinPoint);
        Mockito.verify(storageManager, Mockito.times(1)).saveFile(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(InputStream.class));
        Mockito.verify(storageManager, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertFalse(pageSettingsAction.hasFieldErrors());
        Mockito.verify(pageManager, Mockito.times(1)).updateParams(Mockito.any());
    }

    @Test
    public void executeUpdateSystemParams_3() throws Exception {
        String path = System.getProperty("java.io.tmpdir") + File.separator + "robot.txt";
        this.request.setParameter(PageSettingsActionAspect.PARAM_ROBOT_ALTERNATIVE_PATH_CODE, path);
        this.request.setParameter(PageSettingsActionAspect.PARAM_ROBOT_CONTENT_CODE, "Robot content");
        actionAspect.executeUpdateSystemParams(joinPoint);
        Mockito.verify(storageManager, Mockito.times(0)).saveFile(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(InputStream.class));
        Mockito.verify(storageManager, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertFalse(pageSettingsAction.hasFieldErrors());
        Mockito.verify(pageManager, Mockito.times(1)).updateParams(Mockito.any());
    }

    @Test
    public void executeUpdateSystemParams_4() throws Exception {
        String path = System.getProperty("java.io.tmpdir") + File.separator + "meta-inf" + File.separator + "robot.txt";
        this.request.setParameter(PageSettingsActionAspect.PARAM_ROBOT_ALTERNATIVE_PATH_CODE, path);
        this.request.setParameter(PageSettingsActionAspect.PARAM_ROBOT_CONTENT_CODE, "Robot content");
        actionAspect.executeUpdateSystemParams(joinPoint);
        Mockito.verify(storageManager, Mockito.times(0)).saveFile(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(InputStream.class));
        Mockito.verify(storageManager, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertTrue(pageSettingsAction.hasFieldErrors());
        Assertions.assertEquals(1, pageSettingsAction.getFieldErrors().get(PageSettingsActionAspect.PARAM_ROBOT_ALTERNATIVE_PATH_CODE).size());
        Mockito.verify(pageManager, Mockito.times(1)).updateParams(Mockito.any());
    }

    @Test
    public void executeUpdateSystemParams_9() throws EntException {
        Mockito.doThrow(EntException.class).when(pageManager).updateParams(Mockito.any());
        actionAspect.executeUpdateSystemParams(joinPoint);
        Mockito.verify(storageManager, Mockito.times(1)).deleteFile(Mockito.anyString(), Mockito.anyBoolean());
        Assertions.assertFalse(pageSettingsAction.hasFieldErrors());
        Assertions.assertTrue(pageSettingsAction.hasActionErrors());
    }

}
