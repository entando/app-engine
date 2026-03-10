<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>

<!-- Admin console Breadcrumbs -->
<ol class="breadcrumb page-tabs-header breadcrumb-position">
    <li><s:text name="title.pageDesigner" /></li>
    <li>
        <s:url action="configure" namespace="/do/Page" var="configureURL">
            <s:param name="pageCode"><s:property value="currentPage.code"/></s:param>
        </s:url>
        <s:set var="configureTitle">
            <s:text name="note.goToSomewhere" />: <s:text name="title.configPage" />
        </s:set>
        <a href="${configureURL}" title="${configureTitle}"><s:text name="title.configPage" /></a>
    </li>
    <li class="page-title-container"><s:text name="name.widget" /></li>
</ol>

<!-- Page Title -->
<s:set var="dataContent" value="%{'help block'}" />
<s:set var="dataOriginalTitle" value="%{'Section Help'}"/>
<h1 class="page-title-container">
	<s:text name="name.widget" />
	<span class="pull-right">
        <a tabindex="0" role="button" data-toggle="popover" data-trigger="focus" data-html="true" data-content="<s:text name="name.widget.help" />" data-placement="left" data-original-title="">
            <span class="fa fa-question-circle-o" aria-hidden="true"></span>
        </a>
    </span>
</h1>

<hr>

<div id="main" role="main">

	<!-- Info Details  -->
	<div class="button-bar mt-20">
		<wpsa:action namespace="/do/Page" name="printPageDetails"
					 executeResult="true" ignoreContextParams="true">
			<s:param name="selectedNode" value="currentPage.code"></s:param>
		</wpsa:action>
	</div>

	<!-- Form -->
	<s:form action="saveConfig" namespace="/do/jpwebdynamicform/Page/SpecialWidget/Webdynamicform"
			cssClass="form-horizontal">
		<div class="panel panel-default mt-20">
			<div class="panel-heading">
				<s:include value="/WEB-INF/apsadmin/jsp/portal/include/frameInfo.jsp" />
			</div>
			<div class="panel-body">
				<h2 class="h5 margin-small-vertical">
					<label class="sr-only"><s:text name="name.widget" /></label>
					<span class="icon fa fa-puzzle-piece" title="<s:text name="name.widget" />"></span>&#32;
					<s:property value="%{getWidgetTypeTitle(showlet.typeCode)}" />
				</h2>

				<p class="sr-only">
					<wpsf:hidden name="pageCode" />
					<wpsf:hidden name="frame" />
					<wpsf:hidden name="widgetTypeCode" value="%{showlet.typeCode}" />
				</p>

				<!-- Form errors -->
				<s:if test="hasErrors()">
					<div class="alert alert-danger alert-dismissable">
						<button type="button" class="close" data-dismiss="alert" aria-hidden="true">
							<span class="pficon pficon-close"></span>
						</button>
						<span class="pficon pficon-error-circle-o"></span>
						<h3 class="h4 margin-none"><s:text name="message.title.FieldErrors" /></h3>
						<ul class="margin-base-vertical">
						<s:if test="hasActionErrors()">
							<s:iterator value="actionErrors">
								<li><s:property escapeHtml="false"/></li>
							</s:iterator>
						</s:if>
						<s:if test="hasFieldErrors()">
							<s:iterator value="fieldErrors">
								<s:iterator value="value">
									<li><s:property escapeHtml="false" /></li>
								</s:iterator>
							</s:iterator>
						</s:if>
						</ul>
					</div>
				</s:if>

				<fieldset class="col-xs-12 no-padding">
					<legend><s:text name="label.info" /></legend>
					<div class="form-group">
						<label class="col-sm-2 control-label" for="jpwebdynamicform_typecode">
							<s:text name="label.typeCode"/>
						</label>
						<div class="col-sm-10">
							<wpsf:select id="jpwebdynamicform_typecode" name="typeCode" list="messageTypes" listKey="code" listValue="descr" cssClass="form-control"/>
						</div>
					</div>
					<div class="form-group">
						<label class="col-sm-2 control-label" for="jpwebdynamicform_formProtectionType">
							<s:text name="label.formProtectionType"/>
						</label>
						<div class="col-sm-10">
							<wpsf:select id="jpwebdynamicform_formProtectionType"  name="formProtectionType" list="formProtectionTypeSelectItems" listKey="key" listValue="value" cssClass="form-control"/>
						</div>
					</div>
				</fieldset>
			</div>
		</div>

		<div class="row">
			<div class="col-xs-12">
				<wpsf:submit type="button" cssClass="btn btn-primary pull-right">
					<s:text name="label.save" />
				</wpsf:submit>
			</div>
		</div>
	</s:form>
</div>
