<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<@s.if test="#attribute.required">
    <abbr class="icon icon-asterisk" title="<@wp.i18n key="jpwebdynamicform_ENTITY_ATTR_FLAG_MANDATORY_FULL" />"><span class="noscreen"><@wp.i18n key="jpwebdynamicform_ENTITY_ATTR_FLAG_MANDATORY_SHORT" /></span></abbr>
</@s.if>