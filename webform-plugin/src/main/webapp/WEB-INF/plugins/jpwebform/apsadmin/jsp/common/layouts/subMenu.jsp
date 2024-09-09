<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<wp:ifauthorized permission="superuser">
	<li><a href="<s:url namespace="/do/jpwebform/Form" action="list" />" ><s:text name="jpwebform.title.formManagement" /></a></li>
</wp:ifauthorized>
