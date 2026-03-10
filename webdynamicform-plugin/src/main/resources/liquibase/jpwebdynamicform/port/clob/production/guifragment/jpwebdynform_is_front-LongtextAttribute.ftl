<#assign s=JspTaglibs["/struts-tags"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>
<@s.if test="#lang.default">
    <@wpsf.textarea useTabindexAutoIncrement=true cols="30" rows="5" id="%{attribute_id}" name="%{#attributeTracer.getFormFieldName(#attribute)}" value="%{#attribute.getTextForLang(#lang.code)}" />
</@s.if>