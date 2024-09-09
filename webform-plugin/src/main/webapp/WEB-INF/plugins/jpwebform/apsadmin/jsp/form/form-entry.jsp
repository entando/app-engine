<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>
<h1 class="panel panel-default title-page">
	<span class="panel-body display-block">
		<a href="<s:url action="list" />"><s:text name="jpwebform.title.formManagement" /></a>
		&#32;/&#32;
		<s:if test="getStrutsAction() == 1">
			<s:text name="jpwebform.form.label.new" />
		</s:if>
		<s:elseif test="getStrutsAction() == 2">
			<s:text name="jpwebform.form.label.edit" />
		</s:elseif>
	</span>
</h1>
	<s:form action="save" cssClass="form-horizontal">
	<s:if test="hasFieldErrors()">
		<div class="alert alert-danger alert-dismissable fade in">
			<button type="button" class="close" data-dismiss="alert"><span class="icon fa fa-times"></span></button>
			<h2 class="h4 margin-none"><s:text name="message.title.FieldErrors" /></h2>
		</div>
	</s:if>
	<s:if test="hasActionErrors()">
		<div class="alert alert-danger alert-dismissable fade in">
			<button class="close" data-dismiss="alert"><span class="icon fa fa-times"></span></button>
			<h2 class="h4 margin-none"><s:text name="message.title.ActionErrors" /></h2>
			<ul class="margin-base-top">
				<s:iterator value="actionErrors">
					<li><s:property escapeHtml="false" /></li>
				</s:iterator>
			</ul>
		</div>
	</s:if>

	<p class="sr-only">
		<wpsf:hidden name="strutsAction" />
	<s:if test="getStrutsAction() == 2">
		<wpsf:hidden name="id" />
	</s:if>
	</p>

	<%-- name --%>
		<s:set var="fieldFieldErrorsVar" value="%{fieldErrors['name']}" />
		<s:set var="fieldHasFieldErrorVar" value="#fieldFieldErrorsVar != null && !#fieldFieldErrorsVar.isEmpty()" />
		<s:set var="controlGroupErrorClassVar" value="%{#fieldHasFieldErrorVar ? ' has-error' : ''}" />
		<div class="form-group<s:property value="#controlGroupErrorClassVar" />">
			<div class="col-xs-12">
				<label for="form_name"><s:text name="label.name" /></label>
				<wpsf:textfield name="name" id="form_name" cssClass="form-control" />
				<s:if test="#fieldHasFieldErrorVar">
					<span class="help-block text-danger">
						<s:iterator value="%{#fieldFieldErrorsVar}"><s:property />&#32;</s:iterator>
					</span>
				</s:if>
			</div>
		</div>
	<%-- submitted --%>
		<s:set var="fieldFieldErrorsVar" value="%{fieldErrors['submitted']}" />
		<s:set var="fieldHasFieldErrorVar" value="#fieldFieldErrorsVar != null && !#fieldFieldErrorsVar.isEmpty()" />
		<s:set var="controlGroupErrorClassVar" value="%{#fieldHasFieldErrorVar ? ' has-error' : ''}" />
		<div class="form-group<s:property value="#controlGroupErrorClassVar" />">
			<div class="col-xs-12">
				<label for="form_submitted"><s:text name="label.submitted" /></label>
				<wpsf:textfield name="submitted" id="form_submitted" cssClass="form-control datepicker" />
				<s:if test="#fieldHasFieldErrorVar">
					<span class="help-block text-danger">
						<s:iterator value="%{#fieldFieldErrorsVar}"><s:property />&#32;</s:iterator>
					</span>
				</s:if>
			</div>
		</div>
	<%-- data --%>
		<s:set var="fieldFieldErrorsVar" value="%{fieldErrors['data']}" />
		<s:set var="fieldHasFieldErrorVar" value="#fieldFieldErrorsVar != null && !#fieldFieldErrorsVar.isEmpty()" />
		<s:set var="controlGroupErrorClassVar" value="%{#fieldHasFieldErrorVar ? ' has-error' : ''}" />
		<div class="form-group<s:property value="#controlGroupErrorClassVar" />">
			<div class="col-xs-12">
				<label for="form_data"><s:text name="label.data" /></label>
				<wpsf:textfield name="data" id="form_data" cssClass="form-control" />
				<s:if test="#fieldHasFieldErrorVar">
					<span class="help-block text-danger">
						<s:iterator value="%{#fieldFieldErrorsVar}"><s:property />&#32;</s:iterator>
					</span>
				</s:if>
			</div>
		</div>

	<%-- save button --%>
	<div class="form-group">
		<div class="col-xs-12 col-sm-4 col-md-3 margin-small-vertical">
			<wpsf:submit type="button" action="save" cssClass="btn btn-primary btn-block">
				<span class="icon fa fa-floppy-o"></span>&#32;
				<s:text name="label.save" />
			</wpsf:submit>
		</div>
	</div>

	</s:form>

</div>
