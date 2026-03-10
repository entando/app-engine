<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<wp:ifauthorized permission="superuser" var="isSuperUser" />
<wp:ifauthorized permission="jpwebdynamicform_manageForms" var="isManageForms" />

<c:if test="${isSuperUser || isManageForms}">
    <!-- Web Dynamic Form -->
    <li class="list-group-item secondary-nav-item-pf" data-target="#apps-jpwebdynamicform">
        <a>
            <span class="fa fa-rocket" data-toggle="tooltip" title="<s:text name="jpwebdynamicform.name" />"></span>
            <span class="list-group-item-value"><s:text name="jpwebdynamicform.name" /></span>
        </a>
        <!--Integrations secondary-->
        <div id="apps-jpwebdynamicform" class="nav-pf-secondary-nav">
            <div class="nav-item-pf-header">
                <a class="secondary-collapse-toggle-pf" data-toggle="collapse-secondary-nav"></a>
                <span><s:text name="jpwebdynamicform.name" /></span>
            </div>
            <ul class="list-group">
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
            </ul>
        </div>
    </li>
</c:if>
