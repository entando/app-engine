<#assign wp=JspTaglibs["/aps-core"]>
<@wp.pageWithWidget var="searchResultPageVar" widgetTypeCode="search_result" />
<#include "entando_ootb_carbon_include" >
<search-bar-widget
   action-url="<#if (searchResultPageVar??) ><@wp.url page="${searchResultPageVar.code}" /></#if>"
   placeholder="<@wp.i18n key="ESSF_SEARCH" />"
></search-bar-widget>