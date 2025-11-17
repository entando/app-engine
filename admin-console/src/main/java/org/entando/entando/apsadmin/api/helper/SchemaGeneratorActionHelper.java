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
package org.entando.entando.apsadmin.api.helper;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;

import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.aps.system.services.api.model.StringApiResponse;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;


import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.aps.util.FileTextReader;

/**
 * @author E.Santoboni
 */
public class SchemaGeneratorActionHelper {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(SchemaGeneratorActionHelper.class);
	
    public Class extractResponseClass(ApiMethod method, HttpServletRequest request) throws EntException {
        Class responseClass = null;
        try {
            if (null != method.getResponseClassName()) {
                responseClass = Class.forName(method.getResponseClassName());
            } else {
                responseClass = this.extractReturnType(method, request);
            }
            if (null == responseClass) {
                responseClass = StringApiResponse.class;
            }
        } catch (Throwable t) {
        	_logger.error("Error extracting response Class", t);
            //ApsSystemUtils.logThrowable(t, this, "extractResponseClass", "Error extracting response Class");
            throw new EntException("Error extracting response Class", t);
        }
        return responseClass;
    }
    
    protected Class extractReturnType(ApiMethod method, HttpServletRequest request) throws EntException {
        Class returnType = null;
        try {
            Object bean =  ApsWebApplicationUtils.getBean(method.getSpringBean(), request);
            if (null == bean) {
                throw new EntException("Null bean '" + method.getSpringBean()
                        + "' from method " + method.getHttpMethod() + " of resource " + method.getResourceName());
            }
            Class beanClass = bean.getClass();
            String methodName = method.getSpringBeanMethod();
            Method beanMethod = null;
            if (method.getHttpMethod().equals(ApiMethod.HttpMethod.GET) || method.getHttpMethod().equals(ApiMethod.HttpMethod.DELETE)) {
                Class[] parameterTypes = new Class[]{Properties.class};
                beanMethod = beanClass.getDeclaredMethod(methodName, parameterTypes);
            } else {
                Class expectedType = method.getExpectedType();
                if (null == expectedType) {
                    throw new EntException("Null expectedType for Method " + method.getHttpMethod() + " for resource " + method.getResourceName());
                }
                try {
                    //special case - put or post method with properties
                    Class[] parameterTypes = new Class[]{expectedType, Properties.class};
                    beanMethod = beanClass.getDeclaredMethod(methodName, parameterTypes);
                } catch (Exception e) {
                    //nothing to catch
                }
                if (null == beanMethod) {
                    Class[] parameterTypes2 = new Class[]{expectedType};
                    beanMethod = beanClass.getDeclaredMethod(methodName, parameterTypes2);
                }
            }
            if (null != beanMethod) {
                returnType = beanMethod.getReturnType();
            }
            if (null != returnType && returnType.getName().equals("void")) {
                return null;
            }
        } catch (Throwable t) {
        	_logger.error("Error extracting return type", t);
            //ApsSystemUtils.logThrowable(t, this, "extractReturnType", "Error extracting return type ");
            return null;
        }
        return returnType;
    }
    
    public String generateSchema(Class jaxbObject) throws Exception {
        InputStream mainStream = null;
        String schema = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(jaxbObject);
            StreamSchemaOutputResolver sor = new StreamSchemaOutputResolver();
            jaxbContext.generateSchema(sor);
            mainStream = sor.getStream();
            schema = FileTextReader.getText(mainStream);
        } catch (Throwable t) {
        	_logger.error("Error extracting generating schema from class {}", jaxbObject, t);
            //ApsSystemUtils.logThrowable(t, this, "generateSchema", "Error extracting generating schema from class " + jaxbObject);
            throw new RuntimeException("Error extracting generating schema", t);
        } finally {
            if (null != mainStream) mainStream.close();
        }
        return schema;
    }
    
}
