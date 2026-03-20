<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<wp:ifauthorized permission="superuser" var="isSuperUser" />
<wp:ifauthorized permission="jpwebdynamicform_manageForms" var="isManageForms" />

<s:set var="appBuilderBaseURL" ><wp:info key="systemParam" paramName="appBuilderBaseURL" /></s:set>

<c:if test="${isSuperUser || isManageForms}">
    <li style="padding: 5px 0 5px 20px; list-style: none; color: #fff; cursor: default;">
        <strong><s:text name="jpwebdynamicform.name" /></strong>
    </li>
    <li class="list-group-item">
        <a href="<s:url action="list" namespace="/do/jpwebdynamicform/Message/Operator" />">
            <span class="list-group-item-value">
                <s:text name="jpwebdynamicform.menu.messages"/>
            </span>
        </a>
    </li>
    <c:if test="${isSuperUser}">
        <li class="list-group-item">
            <a href="<s:url namespace="/do/jpwebdynamicform/Message/Config" action="list" />">
                <span class="list-group-item-value">
                    <s:text name="jpwebdynamicform.menu.config"/>
                </span>
            </a>
        </li>
        <li class="list-group-item">
            <a href="<s:url namespace="/do/jpwebdynamicform/Entity" action="viewEntityTypes"><s:param name="entityManagerName">jpwebdynamicformMessageManager</s:param></s:url>">
                <span class="list-group-item-value">
                    <s:text name="%{'title.jpwebdynamicformMessageManager.management'}"/>
                </span>
            </a>
        </li>
        <li class="list-group-item">
            <a href="<s:url namespace="/do/jpwebdynamicform/Config" action="systemParams"></s:url>">
                <span class="list-group-item-value">
                    <s:text name="breadcrumb.capcha"/>
                </span>
            </a>
        </li>
    </c:if>
    <li style="list-style: none; border-bottom: 1px solid #4d5258; margin: 5px 5px 5px 25px;"></li>
</c:if>