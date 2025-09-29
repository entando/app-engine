package org.entando.entando.aps.servlet;

import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;

@ExtendWith(MockitoExtension.class)
class XSSRequestWrapperTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private Validator validator;
    @Mock
    private Encoder encoder;

    @Test
    void shouldGetParameterValuesManageException() throws Exception{
        Mockito.when(httpServletRequest.getParameterValues("testParams")).thenReturn(new String[]{"test1", "test2", "test3"});
        Mockito.when(httpServletRequest.getParameter("test1")).thenReturn("value1");
        Mockito.when(httpServletRequest.getParameter("test2")).thenReturn("value2");
        Mockito.when(httpServletRequest.getParameter("test3")).thenReturn("value3");
        Mockito.when(httpServletRequest.getHeader("header1")).thenReturn("headerValue1");
        Mockito.when(httpServletRequest.getHeader("header2")).thenReturn("headerValue2");

        Mockito.doThrow(ValidationException.class)
                .when(validator)
                .getValidInput("HTTP parameter value: value1", "value1", "HTTPParameterValue", 2000, true);
        Mockito.when(validator.getValidInput("HTTP parameter value: value2", "value2", "HTTPParameterValue", 2000, true))
                .thenReturn("value2");
        Mockito.when(validator.getValidInput("HTTP parameter value: value3", "value3", "HTTPParameterValue", 2000, true))
                .thenThrow(new RuntimeException("error value exception"));

        Mockito.doThrow(ValidationException.class)
                .when(validator)
                .getValidInput("HTTP header value: headerValue1", "headerValue1", "HTTPHeaderValue", 150, false);
        Mockito.when(validator.getValidInput("HTTP header value: headerValue2", "headerValue2", "HTTPHeaderValue", 150, false))
                .thenThrow(new RuntimeException("error header exception"));

        Mockito.when(encoder.encodeForHTMLAttribute("value1")).thenReturn("value1");
        Mockito.when(encoder.encodeForHTMLAttribute("headerValue1")).thenReturn("headerValue1");

        try(MockedStatic<ESAPI> esapi = Mockito.mockStatic(ESAPI.class)) {
            esapi.when(() -> ESAPI.validator()).thenReturn(validator);
            esapi.when(() -> ESAPI.encoder()).thenReturn(encoder);

            XSSRequestWrapper xssRequestWrapperToTest = new XSSRequestWrapper(httpServletRequest);
            String[] resultValue = xssRequestWrapperToTest.getParameterValues("testParams");
            Assertions.assertTrue(Stream.of(resultValue).anyMatch("value1"::equals));
            Assertions.assertTrue(Stream.of(resultValue).anyMatch("value2"::equals));
            Assertions.assertTrue(Stream.of(resultValue).anyMatch("value3"::equals));

            String resultHeader = xssRequestWrapperToTest.getHeader("header1");
            Assertions.assertEquals("headerValue1", resultHeader);

            resultHeader = xssRequestWrapperToTest.getHeader("header2");
            Assertions.assertEquals("headerValue2", resultHeader);
        }
    }
}
