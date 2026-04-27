<#assign s=JspTaglibs["/struts-tags"]>
<@s.if test="#lang.default">
    <@s.if test="#attribute.failedDateString == null">
        <@s.set var="dateAttributeValue" value="#attribute.getFormattedDate('yyyy-MM-dd')" />
    </@s.if>
    <@s.else>
        <@s.set var="dateAttributeValue" value="#attribute.failedDateString" />
    </@s.else>
    <input type="date"
        id="<@s.property value="#attribute_id" />"
        data-jpwebdynamicform-date="true"
        name="<@s.property value="#attributeTracer.getFormFieldName(#attribute)" />"
        value="<@s.property value="#dateAttributeValue" />"
        class="jpwebdynamicform-date" />
</@s.if>