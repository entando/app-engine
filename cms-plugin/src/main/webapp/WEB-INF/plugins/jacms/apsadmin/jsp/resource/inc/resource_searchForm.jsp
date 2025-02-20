<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/aps-core" prefix="wp" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>
<s:set var="categoryTreeStyleVar" ><wp:info key="systemParam" paramName="treeStyle_category" /></s:set>
<%--<s:include value="/WEB-INF/apsadmin/jsp/common/layouts/assets-more/category/categoryTree-extra.jsp" />--%>

<s:form action="search" cssClass="form-horizontal" role="search">
    <p class="sr-only">
        <wpsf:hidden name="resourceTypeCode" />
        <s:if test="#categoryTreeStyleVar == 'request'">
            <s:iterator value="treeNodesToOpen" var="treeNodeToOpenVar"><wpsf:hidden name="treeNodesToOpen" value="%{#treeNodeToOpenVar}"/></s:iterator>
        </s:if>
        <wpsf:hidden name="contentOnSessionMarker" />
    </p>
    <div class="searchPanel form-group searchPanelColumn">
        <div class="well">
            <div class="form-group" style="display: flex; align-items: center;">
                <div style="margin-right: 16px;">
                    <wpsf:textfield name="text" id="text" cssClass="form-control"  title="%{getText('label.search.by')+' '+getText('label.description')}" placeholder="%{getText('label.description')}"/>
                </div>
                <div>
                    <div class="form-group">
                        <wpsf:submit type="button" cssClass="btn btn-primary pull-right">
                            <s:text name="label.search" />
                        </wpsf:submit>
                    </div>
                </div>
            </div>
      
            </div>
           
            <div class="panel-group" id="accordion-markup">
                <div class="panel panel-default">
                    <div class="panel-heading" style="padding:0 0 10px;">
                        <p class="panel-title active">
                            <a data-toggle="collapse" data-parent="#accordion-markup" href="#collapseOne">
                                <s:text name="label.search.advanced" />
                            </a>
                        </p>
                    </div>
                    <div id="collapseOne" class="panel-collapse collapse <s:if test="%{#attr['openCollapsed'] || #attr['openCollapsed'].equals('\\'true\\'') || openCollapsed}">in</s:if>">
                            <div class="panel-body" style="display:flex; gap: 8px; justify-content: flex-start; align-items: flex-start;">
                            <%-- groups --%>
                            <s:set var="allowedGroupsVar" value="allowedGroups"></s:set>
                            <s:if test="null != #allowedGroupsVar && #allowedGroupsVar.size()>1">
                                <div class="form-group">
                                    <label for="ownerGroupName" class="control-label" ><s:text name="label.group" /></label>
                                    <div class="col-sm-9" >
                                        <wpsf:select name="ownerGroupName" id="ownerGroupName" list="#allowedGroupsVar" headerKey="" headerValue="%{getText('label.all')}" listKey="name" listValue="descr" cssClass="form-control" />
                                    </div>
                                </div>
                            </s:if>

                            <%-- filename --%>
                            <div class="form-group">
                                <label for="fileName" class="control-label"><s:text name="label.filename" /></label>
                                <div class="col-sm-9">
                                    <wpsf:textfield name="fileName" id="fileName" cssClass="form-control"/>
                                </div>
                            </div>

                            <%-- resource id --%>
                            <div class="form-group">
                                <label for="fileName" class="control-label"><s:text name="label.resourceId" /></label>
                                <div class="col-sm-9">
                                    <wpsf:textfield name="searchedResourceId" id="searchedResourceId" cssClass="form-control"/>
                                </div>
                            </div>

                            <%-- category tree --%>
                            <div class="form-group" style="align-items: flex-start;" >
                                <label for="fileName" class="control-label" style="margin-top: 6px;"><s:text name="label.categoriesTree" /></label>
                                <div class="col-sm-9">
                                    <div class="table-responsive ">
                                        <table id="categoryTree" class="table table-bordered table-hover table-treegrid <s:property value="#categoryTreeStyleVar" />">
                                            <thead>
                                                <tr>
                                                    <th>
                                                        <s:text name="label.categoriesTree" />
                                                        <s:if test="#categoryTreeStyleVar == 'classic'">
                                                            <button type="button" class="btn-no-button expand-button" id="expandAll">
                                                                <span class="fa fa-plus-square-o treeInteractionButtons" aria-hidden="true"></span>&#32;<s:text name="label.expandAll" />
                                                            </button>
                                                            <button type="button" class="btn-no-button" id="collapseAll">
                                                                <span class="fa fa-minus-square-o treeInteractionButtons" aria-hidden="true"></span>&#32;<s:text name="label.collapseAll" />
                                                            </button>
                                                        </s:if>
                                                    </th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <s:set var="inputFieldName" value="'categoryCode'" />
                                                <s:set var="selectedTreeNode" value="categoryCode" />
                                                <s:set var="liClassName" value="'category'" />
                                                <s:set var="treeItemIconName" value="'fa-folder'" />
                                                <s:if test="%{#categoryTreeStyleVar == 'classic'}">
                                                    <s:set var="currentRoot" value="allowedTreeRootNode" />
                                                    <s:include value="/WEB-INF/apsadmin/jsp/common/treeBuilderCategories.jsp" />
                                                </s:if>
                                                <s:elseif test="%{#categoryTreeStyleVar == 'request'}">
                                                    <s:set var="currentRoot" value="showableTree" />
                                                    <s:set var="openTreeActionName" value="'openCloseCategoryTreeNodeOnResourceFinding'" />
                                                    <s:set var="closeTreeActionName" value="'openCloseCategoryTreeNodeOnResourceFinding'" />
                                                    <s:set var="skipJoinAction" value="'true'" />
                                                    <s:include value="/WEB-INF/apsadmin/jsp/common/treeBuilder-request-categories.jsp" />
                                                </s:elseif>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
        </div>
    </div>
</s:form>
