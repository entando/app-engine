<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsa=JspTaglibs["/apsadmin-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>

<@s.set var="titleKey">jpwebdynamicform_TITLE_<@s.property value="typeCode"/></@s.set>
<@s.set var="typeCodeKey" value="typeCode" />
<@s.set var="lang" value="defaultLang" />
<h1><@wp.i18n key="${titleKey}" /></h1>
<form id="jpwebdynamicform_form" class="form-horizontal jpwebdynamicform-<@s.property value="#typeCodeKey" />" action="<@wp.action path="/ExtStr2/do/jpwebdynamicform/Message/User/send.action"/>" method="post">
    <@s.if test="hasFieldErrors()">
        <div class="alert alert-block">
            <p><strong><@wp.i18n key="ERRORS"/></strong></p>
            <ul class="unstyled">
                <@s.iterator value="fieldErrors">
                    <@s.iterator value="value">
                        <li><@s.property escapeHtml=false /></li>
                    </@s.iterator>
                </@s.iterator>
            </ul>
        </div>
    </@s.if>
    <@s.if test="hasActionErrors()">
        <div class="alert alert-block">
            <p><strong><@wp.i18n key="ERRORS"/></strong></p>
            <ul class="unstyled">
                <@s.iterator value="actionErrors">
                    <li><@s.property escapeHtml=false /></li>
                </@s.iterator>
            </ul>
        </div>
    </@s.if>
    <@s.set var="hasFieldErrors" value="%{hasFieldErrors()}" />
    <@s.set var="fieldErrors" value="%{fieldErrors}" />
    <p class="noscreen hide">
        <@wpsf.hidden name="typeCode" />
    </p>
    <@s.if test="honeypotEnabled">
        <p class="noscreen">
            <@wp.i18n key="jpwebdynamicform_${honeypotParamName}" /><br />
            <@wpsf.textfield name="%{honeypotParamName}" id="" maxlength=254 />
        </p>
    </@s.if>
    <p>
        <@wp.i18n key="jpwebdynamicform_INFO" />
    </p>
    <@s.iterator value="message.attributeList" var="attribute">
        <@wpsa.tracerFactory var="attributeTracer" lang="%{#lang.code}" />
        <@s.set var="i18n_attribute_name" value="%{'jpwebdynamicform_'+ #typeCodeKey +'_'+ #attribute.name}" scope="request" />
        <@s.set var="attribute_id" value="%{'jpwebdynamicform_'+ #typeCodeKey +'_'+ #attributeTracer.getFormFieldName(#attribute)}" />
        <@s.set var="fieldErrorClass" value="%{#fieldErrors.containsKey(#attributeTracer.getFormFieldName(#attribute)) ? ' error ' : ' '  }" />
        <@s.if test="#attribute.type != 'Composite'">
            <div class="control-group <@s.property value="%{' attribute-type-'+#attribute.type+' '}" /> <@s.property value="#fieldErrorClass" />">
                <label class="control-label" for="<@s.property value="#attribute_id" />">
                    <@wp.i18n key="${i18n_attribute_name}" />
                    <@wp.fragment code="jpwebdynform_is_front_AttributeInfo" escapeXml=false />
                </label>
                <div class="controls">
                    <@s.if test="#attribute.type == 'Boolean'">
                        <@wp.fragment code="jpwebdynform_is_front-BooleanAttribute" escapeXml=false />
                    </@s.if>
                    <@s.elseif test="#attribute.type == 'CheckBox'">
                        <@wp.fragment code="jpwebdynform_is_front-CheckboxAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'Date'">
                        <@wp.fragment code="jpwebdynform_is_front-DateAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'Enumerator'">
                        <@wp.fragment code="jpwebdynform_is_front-EnumeratorAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'EnumeratorMap'">
                        <@wp.fragment code="jpwebdynform_is_front-EnumeratorMapAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'Longtext'">
                        <@wp.fragment code="jpwebdynform_is_front-LongtextAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'Number'">
                        <@wp.fragment code="jpwebdynform_is_front-NumberAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'Monotext' || #attribute.type == 'Text'">
                        <@wp.fragment code="jpwebdynform_is_front-MonotextAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.elseif test="#attribute.type == 'ThreeState'">
                        <@wp.fragment code="jpwebdynform_is_front-ThreeStateAttribute" escapeXml=false />
                    </@s.elseif>
                    <@s.else>
                        <@wp.fragment code="jpwebdynform_is_front-MonotextAttribute" escapeXml=false />
                    </@s.else>
                    <@wp.fragment code="jpwebdynform_is_front_attributeInfo-help-block" escapeXml=false />
                </div>
            </div>
        </@s.if>
        <@s.else>
            <div class="well well-small">
                <fieldset class=" <@s.property value="%{' attribute-type-'+#attribute.type+' '}" /> ">
                    <legend class="margin-medium-top"><@wp.i18n key="${i18n_attribute_name}" />
                        <@wp.fragment code="jpwebdynform_is_front_AttributeInfo" escapeXml=false />
                        <@wp.fragment code="jpwebdynform_is_front_attributeInfo-help-block" escapeXml=false />
                        <@wp.fragment code="jpwebdynform_is_front-CompositeAttribute" escapeXml=false />
                </fieldset>
            </div>
        </@s.else>
    </@s.iterator>
    <@s.if test="recaptchaEnabled">
        <@wp.fragment code="jpwebdynamicform_is_captchaInclude" escapeXml=false />
    </@s.if>
    <p class="form-actions">
        <@wp.i18n key="jpwebdynamicform_INVIA" var="labelSubmit" />
        <@wpsf.submit
        cssClass="btn btn-primary"
        useTabindexAutoIncrement=true
        value="%{#attr.labelSubmit}" />
    </p>
</form>