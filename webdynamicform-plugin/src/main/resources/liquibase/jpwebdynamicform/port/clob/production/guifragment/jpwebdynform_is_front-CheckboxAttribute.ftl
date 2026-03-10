<#assign s=JspTaglibs["/struts-tags"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>
<@s.if test="#lang.default">
    <@s.set var="checkedValueVar" value="%{#attribute.booleanValue != null && #attribute.booleanValue ==true}" />
    <@wpsf.checkbox useTabindexAutoIncrement=true
    name="%{#attributeTracer.getFormFieldName(#attribute)}"
    id="%{attribute_id}" value="#checkedValueVar" />
</@s.if>