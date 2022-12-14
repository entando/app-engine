<#assign c=JspTaglibs["http://java.sun.com/jsp/jstl/core"]>
<#assign jpseo=JspTaglibs["/jpseo-aps-core"]>

<@jpseo.currentPage param="description" var="metaDescrVar" />
<#if (metaDescrVar??)>
<meta name="description" content="<@c.out value="${metaDescrVar}" />" />
</#if>

<#-- EXAMPLE OF meta infos on page -->
<#--
<@jpseo.seoMetaTag key="author" var="metaAuthorVar" />
<#if (metaAuthorVar??)>
<meta name="author" content="<@c.out value="${metaAuthorVar}" />" />
</#if>

<@jpseo.seoMetaTag key="keywords" var="metaKeywords" />
<#if (metaKeywords??)>
<meta name="keywords" content="<@c.out value="${metaKeywords}" />" />
</#if>
-->
