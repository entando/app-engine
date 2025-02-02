<%@ taglib uri="/aps-core" prefix="wp" %>
<wp:info key="startLang" var="startLangCodeVar" />
<wp:info key="systemParam" paramName="notFoundPageCode" var="notFoundPageCodeVar" />
<jsp:forward page="/${startLangCodeVar}/${notFoundPageCodeVar}.page"/>
