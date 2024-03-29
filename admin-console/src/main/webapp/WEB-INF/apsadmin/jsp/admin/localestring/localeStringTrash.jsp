<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="/aps-core" prefix="wp" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>
<%@ taglib uri="/apsadmin-core" prefix="wpsa" %>

<ol class="breadcrumb page-tabs-header breadcrumb-position">
    <li><s:text name="menu.configure"/></li>
    <li>
        <a href="<s:url namespace="/do/LocaleString" action="list" />">
            <s:text name="title.languageAndLabels" />
        </a>
    </li>
    <li>
        <s:text name="title.languageAdmin.labels"/>
    </li>
    <li class="page-title-container">
        <s:text name="title.generalSettings.locale.delete" />
    </li>
</ol>
<h1 class="page-title-container">
    <div>
        <s:text name="title.generalSettings.locale.delete" />
    </div>
</h1>
<div class="text-right">
    <div class="form-group-separator"></div>
</div>
<br>


<div class="text-center">
    <s:form action="delete">

        <p class="sr-only"><wpsf:hidden name="key"/></p>

    <i class="fa fa-exclamation esclamation-big" aria-hidden="true"></i>
    <p class="esclamation-underline"><s:text name="label.delete"/></p>
    <p class="esclamation-underline-text">
        <s:text name="label.delete.confirm"/>&#32;
        <s:property value="key" />&#63;

    </p>
    <div class="text-center margin-large-top">
        <a class="btn btn-default button-fixed-width" href="<s:url namespace="/do/LocaleString" action="list" />">
            <s:text name="label.back" />
        </a>
        <wpsf:submit type="button"
                     cssClass="btn btn-danger button-fixed-width">
            <s:text name="label.delete"/>
        </wpsf:submit>
    </div>
</s:form>
</div>
