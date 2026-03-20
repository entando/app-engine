<#assign c=JspTaglibs["http://java.sun.com/jsp/jstl/core"]>
<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsa=JspTaglibs["/apsadmin-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>

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
        <@s.elseif test="#attribute.type == 'Email'">
            <@wp.fragment code="jpwebdynform_is_front-EmailAttribute" escapeXml=false />
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