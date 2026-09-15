<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<%--
	Mirrors the 'jacms_content_viewer_list_userfilter_ent_CheckBox' guifragment
	(clob/production/guifragment_14_2.ftl) and must stay in step with it: the Freemarker fragment is
	used when present, this JSP is the fallback.

	This was a single "Yes" checkbox, so it could only ever ask for true. It is now the same
	Any/Yes/No group as the Boolean filter - filtering for false is newly possible.
--%>

<c:set var="formFieldNameVar" value="${userFilterOptionVar.formFieldNames[0]}" />
<c:set var="formFieldValue" value="${userFilterOptionVar.formFieldValues[formFieldNameVar]}" />
<c:set var="i18n_Attribute_Key" value="${userFilterOptionVar.attribute.name}" />

<fieldset>
<legend><wp:i18n key="${i18n_Attribute_Key}" /></legend>

<div class="control-group">
	<div class="controls">
		<label for="any_<c:out value="${formFieldNameVar}" />" class="radio">
		<input name="<c:out value="${formFieldNameVar}" />" id="any_<c:out value="${formFieldNameVar}" />" <c:if test="${empty formFieldValue}">checked="checked"</c:if> value="" type="radio" />
		<wp:i18n key="ANY"/></label>
	</div>
	<div class="controls">
		<label for="true_<c:out value="${formFieldNameVar}" />" class="radio">
		<input name="<c:out value="${formFieldNameVar}" />" id="true_<c:out value="${formFieldNameVar}" />" <c:if test="${null != formFieldValue && formFieldValue == 'true'}">checked="checked"</c:if> value="true" type="radio" />
		<wp:i18n key="YES"/></label>
	</div>
	<div class="controls">
		<label for="false_<c:out value="${formFieldNameVar}" />" class="radio">
		<input name="<c:out value="${formFieldNameVar}" />" id="false_<c:out value="${formFieldNameVar}" />" <c:if test="${null != formFieldValue && formFieldValue == 'false'}">checked="checked"</c:if> value="false" type="radio" />
		<wp:i18n key="NO"/></label>
	</div>
</div>

</fieldset>
