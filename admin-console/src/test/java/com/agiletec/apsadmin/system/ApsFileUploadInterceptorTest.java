package com.agiletec.apsadmin.system;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.baseconfig.FileUploadUtils;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApsFileUploadInterceptorTest {

    private static final long MAX_SIZE = 52428800l;

    @Mock
    private ActionInvocation actionInvocation;
    @Mock
    private ConfigInterface configManager;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private ActionContext context;

    @Spy
    private ApsFileUploadInterceptor interceptor;

    @Test
    void shouldSetConfiguredMaxSizeOnlyOnce() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> apsWebApplicationUtils =
                Mockito.mockStatic(ApsWebApplicationUtils.class);
                MockedStatic<FileUploadUtils> fileUploadUtils = Mockito.mockStatic(FileUploadUtils.class);
                MockedStatic<ActionContext> actionContext = Mockito.mockStatic(ActionContext.class)) {

            apsWebApplicationUtils.when(() -> ApsWebApplicationUtils.getBean(
                    Mockito.eq(SystemConstants.BASE_CONFIG_MANAGER), Mockito.any(HttpServletRequest.class))
            ).thenReturn(configManager);

            actionContext.when(ActionContext::getContext).thenReturn(context);
            Mockito.when(actionInvocation.getInvocationContext()).thenReturn(context);
            Mockito.when(context.getServletRequest()).thenReturn(servletRequest);

            fileUploadUtils.when(() -> FileUploadUtils.getFileUploadMaxSize(Mockito.any())).thenReturn(MAX_SIZE);

            // Should set the value at first invocation
            interceptor.intercept(actionInvocation);
            Mockito.verify(interceptor, Mockito.times(1)).setMaximumSize(MAX_SIZE);

            // Should not set the value at second invocation
            interceptor.intercept(actionInvocation);
            Mockito.verify(interceptor, Mockito.times(1)).setMaximumSize(MAX_SIZE);
        }
    }
}
