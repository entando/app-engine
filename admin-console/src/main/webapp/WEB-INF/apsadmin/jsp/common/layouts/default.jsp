<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>
<!DOCTYPE html>
<!--[if IE 9]><html lang="en-us" class="ie9 layout-pf layout-pf-fixed"><![endif]-->
<!--[if gt IE 9]><!-->
<html lang="en-us" class="layout-pf layout-pf-fixed">
    <!--<![endif]-->

    <head>
        <title>Entando - <s:set var="documentTitle"><tiles:getAsString name="title"/></s:set><s:property value="%{getText(#documentTitle)}" escapeHtml="false" /></title>
            <meta charset="UTF-8">
            <meta http-equiv="X-UA-Compatible" content="IE=Edge">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="shortcut icon" href="<wp:resourceURL ignoreTenant="true" />administration/img/favicon-entando.png">
            <link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap" rel="stylesheet">

        <jsp:include page="/WEB-INF/apsadmin/jsp/common/inc/header-include.jsp" />

        <tiles:insertAttribute name="extraResources"/>
    </head>

    <body>

        <!--barra di navigazione-->
        <nav class="navbar navbar-pf-vertical">
            <tiles:insertAttribute name="header"/>
        </nav>
        <!--/.navbar-->

        <!--barra di menu laterale-->
        <div class="nav-pf-vertical nav-pf-vertical-with-sub-menus nav-pf-vertical-collapsible-menus ">

		<s:set var="appBuilderIntegrationEnabled" ><wp:info key="systemParam" paramName="appBuilderIntegrationEnabled" /></s:set>
		<s:if test="#appBuilderIntegrationEnabled == 'true'">
        	<tiles:insertAttribute name="menuAppBuilder"/>
        </s:if>
        <s:else>
            <tiles:insertAttribute name="menu"/>
        </s:else>

        </div>

        <div class="container-fluid container-cards-pf container-pf-nav-pf-vertical">
            <tiles:insertAttribute name="body"/>
        </div>
    </body>
</html>
