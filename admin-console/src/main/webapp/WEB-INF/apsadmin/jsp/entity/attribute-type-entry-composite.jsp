<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>
<%@ taglib uri="/aps-core" prefix="wp" %>


<ol class="breadcrumb page-tabs-header breadcrumb-position">
    <s:text name="%{'title.' + entityManagerName + '.menu'}"/>
    <li><a href="<s:url action="initViewEntityTypes" namespace="/do/Entity"><s:param name="entityManagerName"><s:property value="entityManagerName" /></s:param></s:url>" title="<s:text name="note.goToSomewhere" />:
           <s:text name="title.entityAdmin.manager" />&#32;<s:property value="entityManagerName" />">
    </li>
    <li><s:text name="%{'title.' + entityManagerName + '.management'}" /></a>
    <li class="page-title-container">
        <s:text name="title.entityTypes.editType.edit" />
        : <s:property value="entityType.typeCode" /> - <s:property value="entityType.typeDescr" />
    </li>
</ol>

<h1 class="page-title-container">
    <s:text name="title.entityTypes.editType.edit" />
    : <s:property value="entityType.typeCode" /> - <s:property value="entityType.typeDescr" />
    <span class="pull-right">
        <a tabindex="0" role="button" data-toggle="popover" data-trigger="focus" data-html="true" title="" data-content="<s:text name="%{'page.' + entityManagerName + '.help'}"/>" data-placement="left" data-original-title=""><i class="fa fa-question-circle-o" aria-hidden="true"></i></a>
    </span>
</h1>

<div class="text-right">
    <div class="form-group-separator"><s:text name="label.requiredFields" /></div>
</div>
<br>

<div id="main" role="main">

    <s:form action="saveCompositeAttribute" cssClass="form-horizontal">

        <s:if test="hasFieldErrors()">
            <div class="alert alert-danger alert-dismissable">
                <button type="button" class="close" data-dismiss="alert" aria-hidden="true">
                    <span class="pficon pficon-close"></span>
                </button>
                <span class="pficon pficon-error-circle-o"></span>
                <s:text name="message.title.FieldErrors" />
                <ul>
                    <s:iterator value="fieldErrors">
                        <s:iterator value="value">
                            <li><s:property escapeHtml="false" /></li>
                            </s:iterator>
                        </s:iterator>
                </ul>
            </div>
        </s:if>

        <s:set var="listAttribute" value="listAttribute" />
        <s:set var="compositeAttribute" value="compositeAttributeOnEdit" />

        <div class="alert alert-success">
            <span class="pficon pficon-info"></span>
            <s:text name="note.workingOnAttribute" />:&#32;
            <s:if test="null != #listAttribute">
                <s:property value="#compositeAttribute.type" />,&#32;
                <s:text name="note.workingOnAttributeIn" />&#32;
                <s:property value="#listAttribute.name" />&#32;
                ( <s:property value="#listAttribute.type" />)
            </s:if>
            <s:else>
                <s:property value="#compositeAttribute.name" />
            </s:else>
        </div>

        <fieldset class="">
            <legend><s:text name="label.info" /></legend>

            <div class="form-group">
                <label class="col-sm-2 control-label" for="compositeAttribute.type"><s:text name="label.type" /></label>
                <div class="col-sm-10">
                    <wpsf:textfield cssClass="form-control" id="compositeAttribute.type" name="compositeAttribute.type" value="%{#compositeAttribute.type}" disabled="true" />
                </div>
            </div>
        </fieldset>

        <fieldset><legend><s:text name="label.attributes" /></legend>
            <div class="form-group">
                <label class="col-sm-2 control-label"  for="attributeTypeCode"><s:text name="label.type" />:</label>
                <div class="col-sm-10">
                    <div class="input-group">
                        <wpsf:select list="allowedAttributeElementTypes" headerKey="" headerValue="%{getText('note.choose')}" id="attributeTypeCode" name="attributeTypeCode" listKey="type" listValue="type" cssClass="form-control"/>
                        <span class="input-group-btn">
                            <wpsf:submit type="button" value="%{getText('label.add')}" action="addAttributeElement" cssClass="btn btn-primary" />
                        </span>
                    </div>
                </div>
            </div>

            <s:if test="#compositeAttribute.attributes.size > 0">
                <table class="table table-striped table-bordered table-hover no-mb">
                    <tr>
                        <th class="table-w-20 "><s:text name="label.code" /></th>
                        <th class="table-w-20 "><s:text name="label.type" /></th>
                        <th class="text-center table-w-5 "><s:text name="Entity.attribute.flag.mandatory.full" /></th>
                        <th class="text-center table-w-5 "><s:text name="label.filter" /></th>
                        <th class="text-center table-w-5"><s:text name="label.actions" /></th>
                    </tr>

                    <s:iterator value="#compositeAttribute.attributes" var="attribute" status="elementStatus">
                        <tr>
                            <td><s:property value="#attribute.name" /></td>
                            <td><s:property value="#attribute.type" /></td>
                            <s:set var="flagValue" value="#attribute.required" />
                            <s:set var="flagApplicable" value="true" />
                            <s:set var="flagLabel" value="'Entity.attribute.flag.mandatory.full'" />
                            <s:include value="/WEB-INF/apsadmin/jsp/entity/include/attribute-flag-cell.jsp" />

                            <%-- The searchable flag survives only on boolean-like Composite children
                                 (CompositeAttribute.extractAttributeCompositeElement forces every other
                                 type non-searchable), and a Composite reached through a list is never
                                 indexed per attribute: both cases render as "not applicable". --%>
                            <s:set var="flagValue" value="#attribute.searchable" />
                            <s:set var="flagApplicable" value="%{null == #listAttribute && isNestedSearchableOptionSupported(#attribute.type)}" />
                            <s:set var="flagLabel" value="'label.filter'" />
                            <s:set var="flagUnavailableKey" value="%{null == #listAttribute ? 'Entity.attribute.flag.searchable.notApplicable.type' : 'Entity.attribute.flag.searchable.notApplicable.list'}" />
                            <s:include value="/WEB-INF/apsadmin/jsp/entity/include/attribute-flag-cell.jsp" />
                            <td class="text-center table-view-pf-actions">
                                <div class="dropdown dropdown-kebab-pf">
                                    <button class="btn btn-menu-right dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                        <span class="fa fa-ellipsis-v"></span>
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-right">
                                        <s:set var="elementIndex" value="#elementStatus.index" />
                                        <s:include value="/WEB-INF/apsadmin/jsp/entity/include/attribute-operations-misc-composite.jsp" />
                                        <li>
                                    </ul>
                                </div>
                            </td>
                        </tr>
                    </s:iterator>
                </table>
            </s:if>
        </fieldset>

        <div class="col-xs-12 mt-20">
            <div class="form-group">
                <wpsf:submit type="button" cssClass="btn btn-primary pull-right" action="saveCompositeAttribute" >
                    <s:text name="label.save" />
                </wpsf:submit>
            </div>
        </div>

    </s:form>
</div>
