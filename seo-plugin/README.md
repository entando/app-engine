[![Build Status](https://img.shields.io/endpoint?url=https%3A%2F%2Fstatusbadge-jx.apps.serv.run%2Fentando%2Fentando-plugin-jpseo)](https://github.com/entando/devops-results/tree/logs/jenkins-x/logs/entando/entando-plugin-jpseo/master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=entando_entando-plugin-jpseo&metric=alert_status)](https://sonarcloud.io/dashboard?id=entando_entando-plugin-jpseo)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=entando_entando-plugin-jpseo&metric=coverage)](https://entando.github.io/devops-results/entando-plugin-jpseo/master/jacoco/index.html)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=entando_entando-plugin-jpseo&metric=vulnerabilities)](https://entando.github.io/devops-results/entando-plugin-jpseo/master/dependency-check-report.html)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=entando_entando-plugin-jpseo&metric=code_smells)](https://sonarcloud.io/dashboard?id=entando_entando-plugin-jpseo)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=entando_entando-plugin-jpseo&metric=security_rating)](https://sonarcloud.io/dashboard?id=entando_entando-plugin-jpseo)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=entando_entando-plugin-jpseo&metric=sqale_index)](https://sonarcloud.io/dashboard?id=entando_entando-plugin-jpseo)

entando-plugin-jpseo
# SEO

**Code**: ```jpseo```

## Description

The SEO plugin enables some functionality inside the page configuration (new parameters and friendly url), in the content handling (new role attribute to pilot friendly url) and to extract the sitemap.

## How to use

###### Modify of web.xml

Add, into web.xml file (into src/main/conf folder), the following xml section

```
    <servlet-mapping>
        <servlet-name>ControllerServlet</servlet-name>
        <url-pattern>/page/*</url-pattern>
    </servlet-mapping>
```

###### Page Model CUSTOMIZATION

Add the following tld declaration in the page models:

<%@ taglib prefix="jpseo" uri="/jpseo-aps-core" %>

You can add the following tag (into the header of the pagemodel) to print the entire metatag configuration of the current page

```
<jpseo:seoMetasTag escapeXml="true" /> 
```
OR use the follow instruction for more granular control on the metatag to print.

In order to add the metadata description, add the following section in the header: 
```
<jpseo:currentPage param="description" var="metaDescr" />
<c:if test="${metaDescr!=null}" >
<meta name="description" content="<c:out value="${metaDescr}" />" />
</c:if>
```
you can use, into the header of the page models, the following sections:
```
<jpseo:seoMetaTag key="author" var="metaAuthorVar" />
<c:if test="${metaAuthorVar != null}" >
<meta name="author" content="<c:out value="${metaAuthorVar}" />" />
</c:if>
```
```
<jpseo:seoMetaTag key="keywords" var="metaKeywordsVar" />
<c:if test="${metaKeywordsVar != null}" >
<meta name="keywords" content="<c:out value="${metaKeywordsVar}" />" />
</c:if>
```
###### Content CUSTOMIZATION

To ensure that MAIN CONTENT influences the page's description (see appropriate tag in the page templates section)
the jsp of the WIDGET content_viewer must be changed as follows:

**Add the following tld declaration**

<%@ taglib prefix="jpseo" uri="/jpseo-aps-core" %>

**Modify the content tag**

substitute:
```
<jacms:content .... />
```
for 
```
<jpseo:content publishExtraTitle="true" publishExtraDescription="true" />
```

# Developing against local versions of upstream projects (e.g. admin-console,  entando-engine).

Full instructions on how to develop against local versions of upstream projects are available in the
[entando-parent-bom](https://github.com/entando/entando-core-bom) project. 