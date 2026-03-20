<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>
<@s.if test="#lang.default">
    <@wp.i18n key="jpwebdynamicform_SELECT" var="labelSelectVar" />
    <@wpsf.select useTabindexAutoIncrement=true name="%{#attributeTracer.getFormFieldName(#attribute)}" id="%{attribute_id}" headerKey="" headerValue="%{#labelSelectVar}" list="#attribute.items" value="%{#attribute.getText()}" />
</@s.if>