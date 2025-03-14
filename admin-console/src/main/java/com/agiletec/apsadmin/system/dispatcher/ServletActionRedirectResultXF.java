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
package com.agiletec.apsadmin.system.dispatcher;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionExceptionHandler;
import java.net.URL;
import lombok.Setter;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.result.ServletRedirectResult;
import org.entando.entando.aps.util.UrlUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * Redirect Action Result con ancora.
 * Questo resultType è utilizzato per permettere il redirezionamento ad una url che invoca una specifica action
 * (opzionalmente un namespace) con una specifica ancora.
 * <p>
 * See examples below for an example of how request parameters could be passed in.
 * <p>
 * <!-- END SNIPPET: description -->
 *
 * <b>This result type takes the following parameters:</b>
 * <p>
 * <!-- START SNIPPET: params -->
 *
 * <ul>
 *
 * <li><b>actionName (default)</b> - the name of the action that will be redirect to</li>
 *
 * <li><b>namespace</b> - used to determine which namespace the action is in that we're redirecting to . If namespace is
 * null, this defaults to the current namespace</li>
 *
 * <li><b>supressEmptyParameters</b> - optional boolean (defaults to false) that can prevent parameters with no values
 * from being included in the redirect URL.</li>
 *
 * </ul>
 * <p>
 * <!-- END SNIPPET: params -->
 *
 * <b>Example:</b>
 *
 * <pre><!-- START SNIPPET: example -->
 * &lt;package name="public" extends="struts-default"&gt;
 *     &lt;action name="login" class="..."&gt;
 *         &lt;!-- Redirect to another namespace --&gt;
 *         &lt;result type="redirectActionWithAnchor"&gt;
 *             &lt;param name="actionName"&gt;dashboard&lt;/param&gt;
 *             &lt;param name="namespace"&gt;/secure&lt;/param&gt;
 *             &lt;param name="anchorDest"&gt;lang_it/param&gt;
 *         &lt;/result&gt;
 *     &lt;/action&gt;
 * &lt;/package&gt;
 *
 * <!-- END SNIPPET: example --></pre>
 *
 * @see ActionMapper
 */
@Setter
public class ServletActionRedirectResultXF extends ServletRedirectResult implements ReflectionExceptionHandler {

    private static final long serialVersionUID = 4600313492031227334L;

    /**
     * The default parameter
     */
    public static final String DEFAULT_PARAM = "actionName";

    private static final Logger LOG = LoggerFactory.getLogger(ServletActionRedirectResultXF.class);

    protected String actionName;
    protected String namespace;
    protected String method;
    private String anchorDest;

    public ServletActionRedirectResultXF() {
        super();
    }

    public ServletActionRedirectResultXF(String actionName) {
        this(null, actionName, null, null);
    }

    public ServletActionRedirectResultXF(String actionName, String method) {
        this(null, actionName, method, null);
    }


    public ServletActionRedirectResultXF(String namespace, String actionName, String method) {
        this(namespace, actionName, method, null);
    }

    public ServletActionRedirectResultXF(String namespace, String actionName, String method, String anchor) {
        super(null, anchor);
        this.namespace = namespace;
        this.actionName = actionName;
        this.method = method;
    }

    @Override
    public void execute(ActionInvocation invocation) throws Exception {
        this.actionName = this.conditionalParse(this.actionName, invocation);
        if (this.namespace == null) {
            this.namespace = invocation.getProxy().getNamespace();
        } else {
            this.namespace = this.conditionalParse(this.namespace, invocation);
        }
        if (this.method == null) {
            this.method = "";
        } else {
            this.method = this.conditionalParse(this.method, invocation);
        }

        HttpServletRequest request = ServletActionContext.getRequest();

        URL frontendUrlObject = UrlUtils.determineFrontendUrlObject(request);
        String frontendScheme = frontendUrlObject.toURI().getScheme();
        String frontendServerName = frontendUrlObject.getHost();
        int frontendPort = frontendUrlObject.getPort();

        String sbLocation = frontendScheme + "://"
                + frontendServerName + ((frontendPort != -1) ? ":" + frontendPort : "")
                + request.getContextPath()
                + this.actionMapper.getUriFromActionMapping(new ActionMapping(actionName, namespace, method, null));

        if (null != this.anchorDest) {
            this.setAnchor(this.anchorDest);
        }

        setLocation(sbLocation);

        super.execute(invocation);
    }

    @Override
    protected List<String> getProhibitedResultParams() {
        return Arrays.asList(DEFAULT_PARAM,
                "namespace", "method", "encode", "parse", "location",
                "prependServletContext", "supressEmptyParameters", "anchor", "anchorDest"
        );
    }
}
