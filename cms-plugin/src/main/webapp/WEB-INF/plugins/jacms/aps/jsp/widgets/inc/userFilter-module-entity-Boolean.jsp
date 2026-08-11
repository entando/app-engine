<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<%--
	Mirrors the 'jacms_content_viewer_list_userfilter_ent_Boolean' guifragment
	(clob/production/guifragment_12_2.ftl) and must stay in step with it: the Freemarker fragment is
	used when present, this JSP is the fallback.

	One radio group, "Any" first and selected by default. The separate IGNORE checkbox was dropped:
	it existed only because a radio cannot be deselected once chosen, and a real "Any" option makes
	it redundant. Its include also carried the hidden '_control' field, which no Java reads.
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
