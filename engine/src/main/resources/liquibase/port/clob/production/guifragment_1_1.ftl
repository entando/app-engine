<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsa=JspTaglibs["/apsadmin-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>
<h1><@wp.i18n key="userprofile_EDITPROFILE_TITLE" /></h1>
<#if (Session.currentUser != "guest") >
	<form action="<@wp.action path="/ExtStr2/do/Front/CurrentUser/Profile/save.action" />" method="post" class="form-horizontal">
	<@s.if test="hasFieldErrors()">
		<div class="alert alert-block">
			<p><strong><@wp.i18n key="userprofile_MESSAGE_TITLE_FIELDERRORS" /></strong></p>
			<ul class="unstyled">
				<@s.iterator value="fieldErrors">
					<@s.iterator value="value">
						<li><@s.property escapeHtml=false /></li>
					</@s.iterator>
				</@s.iterator>
			</ul>
		</div>
	</@s.if>
	<@s.set var="lang" value="defaultLang" />
	<@s.iterator value="userProfile.attributeList" var="attribute">
		<@s.if test="%{#attribute.active}">
			<@wpsa.tracerFactory var="attributeTracer" lang="%{#lang.code}" />
				<@s.set var="i18n_attribute_name">userprofile_<@s.property value="userProfile.typeCode" />_<@s.property value="#attribute.name" /></@s.set>
				<@s.set var="attribute_id">userprofile_<@s.property value="#attribute.name" /></@s.set>
				<#include "userprofile_is_IteratorAttribute" >
		</@s.if>
	</@s.iterator>

	<p class="form-actions">
		<@wp.i18n key="userprofile_SAVE_PROFILE" var="userprofile_SAVE_PROFILE" />
		<@wpsf.submit useTabindexAutoIncrement=true value="%{#attr.userprofile_SAVE_PROFILE}" cssClass="btn btn-primary" />
	</p>

	</form>
<#else>
	<p>
		<@wp.i18n key="userprofile_PLEASE_LOGIN" />
	</p>
</#if>