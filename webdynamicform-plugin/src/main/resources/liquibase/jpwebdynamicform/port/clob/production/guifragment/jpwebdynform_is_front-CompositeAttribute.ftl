<#assign c=JspTaglibs["http://java.sun.com/jsp/jstl/core"]>
<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsa=JspTaglibs["/apsadmin-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>

<@s.set var="i18n_parent_attribute_name" value="#attribute.name" />
<@s.set var="masterCompositeAttributeTracer" value="#attributeTracer" />
<@s.set var="masterCompositeAttribute" value="#attribute" />

<@s.iterator value="#attribute.attributes" var="attribute">
    <@s.set var="attributeTracer" value="#masterCompositeAttributeTracer.getCompositeTracer(#masterCompositeAttribute)" />
    <@s.set var="parentAttribute" value="#masterCompositeAttribute" />
    <@s.set var="i18n_attribute_name" value="%{'jpwebdynamicform_'+ #typeCodeKey +'_'+ #attribute.name}" scope="request" />
    <@s.set var="attribute_id" value="%{'jpwebdynamicform_'+ #typeCodeKey +'_'+ #attributeTracer.getFormFieldName(#attribute)}" />
    <@wp.fragment code="jpwebdynform_is_IteratorAttribute" escapeXml=false />
</@s.iterator>
<@s.set var="attributeTracer" value="#masterCompositeAttributeTracer" />
<@s.set var="attribute" value="#masterCompositeAttribute" />
<@s.set var="parentAttribute" value=""></@s.set>