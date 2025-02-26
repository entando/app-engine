<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>

<ol class="breadcrumb page-tabs-header breadcrumb-position">
    <li><s:text name="menu.configure" /></li>
    <li>
        <s:text name="title.categoryManagement" />
    </li>
    <li class="page-title-container">
        <s:text name="title.categorySettings" />
    </li>
</ol>
<div class="page-tabs-header">
    <div class="row" style="display: flex;">
        <div class="col-sm-9" style="display: flex; width: 100%; justify-content: space-between; align-items: center;">
            <h1 style="margin: unset;">
                <s:text name="title.categoryManagement"/>
               
            </h1>
            <span class="pull-right">
                <a tabindex="0" role="button" data-toggle="popover" data-trigger="focus" data-html="true" title=""
                   data-content="<s:text name="page.category.help"/>" data-placement="left" data-original-title="">
                    <i class="fa fa-question-circle-o" aria-hidden="true" style="font-size: 16px;"></i>
                </a>
            </span>
        </div>
        <div class="col-sm-3">
            <ul class="nav nav-tabs nav-justified nav-tabs-pattern">
                <li>
                    <a href="<s:url namespace="/do/Category" action="viewTree" />"><s:text name="title.categoryTree"/></a>
                </li>
                <li class="active">
                    <a href="<s:url namespace="/do/Category" action="configSystemParams" />"><s:text name="title.categorySettings"/></a>
                </li>
            </ul>
        </div>
    </div>
</div>
<br>

<div>
    <s:form action="updateSystemParams">
        <s:if test="hasActionMessages()">
            <div class="alert alert-success">
                <span class="pficon pficon-ok"></span>
                <strong><s:text name="messages.confirm" /></strong>
                <ul>
                    <s:iterator value="actionMessages">
                        <li><s:property escapeHtml="false" /></li>
                        </s:iterator>
                </ul>
            </div>
        </s:if>

        <fieldset>
            <div class="form-group" style="margin-left: -16px;">
                <div class="row" style="display: flex; align-items: center; padding-left: 20px; gap: 16px;">
                    <div>
                        <span for="admin-settings-area-notFoundPageCode"><s:text name="label.chooseYourCategoriesTreeStyle" /></span>
                    </div>
                    <div class="text-left">
                        <s:set var="paramName" value="'treeStyle_category'" />
                        <div class="btn-group" data-toggle="buttons">
                            <label class="btn btn-outlined-secondary <s:if test="systemParams[#paramName] == 'classic'"> active</s:if>">
                                    <wpsf:radio id="classic" name="%{#paramName}" value="classic" checked="%{systemParams[#paramName].equals('classic')}" />
                                <s:text name="treeStyle.classic" />
                            </label>
                            <label class="btn btn-outlined-secondary <s:if test="systemParams[#paramName] == 'request'"> active</s:if>">
                                    <wpsf:radio id="request" name="%{#paramName}" value="request" checked="%{systemParams[#paramName].equals('request')}" />
                                <s:text name="treeStyle.request" />
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </fieldset>
        
        <div class="form-group">
            <div class="col-sm-offset-2">
                <wpsf:submit type="button" cssClass="btn btn-primary pull-right">
                    <s:text name="label.save"/>
                </wpsf:submit>
            </div>
        </div>
    </s:form>
</div>
