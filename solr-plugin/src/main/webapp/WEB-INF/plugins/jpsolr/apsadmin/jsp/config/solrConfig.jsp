<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="wp" uri="/aps-core"%>
<%@ taglib prefix="wpsa" uri="/apsadmin-core"%>
<ol class="breadcrumb page-tabs-header breadcrumb-position">
    <li><s:text name="menu.configure" /></li>
    <li class="page-title-container"><s:text name="title.solrManagement" /></li>
</ol>
<h1 class="page-title-container">
    <div>
        <s:text name="title.solrManagement" />
    </div>
</h1>
<div class="text-right">
    <div class="form-group-separator"></div>
</div>

<s:set value="contentTypesSettings" var="contentTypesSettingsVar" />

<s:set value="%{getLastReloadInfo()}" var="lastReloadInfoVar" />

<s:set value="searcherManagerStatus" var="searcherManagerStatusVar" />

<s:if test="#searcherManagerStatusVar == 1">
    <p class="text-info">
        <a class="btn btn-primary" href="<s:url action="config" namespace="/do/jpsolr" />" title="<s:text name="note.reload.contentIndexes.refresh" />">
            <s:text name="label.refresh" />
        </a>
        &#32;(<s:text name="note.reload.contentIndexes.status.working" />)
    </p>
</s:if>

<s:iterator var="contentTypeSettingsVar" value="#contentTypesSettingsVar">
    
    <h3 class="page-title-container">
    <div>
        <s:property value="#contentTypeSettingsVar.typeCode" />&#32;&ndash;&#32;<s:property value="#contentTypeSettingsVar.typeDescription" />
        <s:if test="%{#searcherManagerStatusVar != 1}" >
        <s:if test="!#contentTypeSettingsVar.valid">
            <span class="pull-right"> 
            <a class="btn btn-primary pull-right"
               href="<s:url namespace="/do/jpsolr" action="refreshType"><s:param name="typeCode" value="#contentTypeSettingsVar.typeCode" /></s:url>" style="margin-bottom: -15px">Refresh&#32;<s:property value="#contentTypeSettingsVar.typeCode" />
            </a>
            </span>
        </s:if>
        <s:else>
            <span class="pull-right"> 
            <a class="btn btn-warning pull-right"
               href="<s:url namespace="/do/jpsolr" action="reloadIndex"><s:param name="typeCode" value="#contentTypeSettingsVar.typeCode" /></s:url>" style="margin-bottom: -15px">Index&#32;<s:property value="#contentTypeSettingsVar.typeCode" />
            </a>
            </span>
            <s:set value="%{null != #lastReloadInfoVar && #lastReloadInfoVar.getDateByType(#contentTypeSettingsVar.typeCode)}" var="indexDateVar" />
            <s:if test="%{#lastReloadInfoVar != null && #lastReloadInfoVar.date == null && #indexDateVar != null}">
                <span>&#32;&ndash;&#32;Last single reload:&#32;<s:date name="#indexDateVar" format="dd/MM/yyyy HH:mm" /></span>
            </s:if>
            <s:elseif test="%{#lastReloadInfoVar != null && #lastReloadInfoVar.date != null}">
                <span>&#32;&ndash;&#32;Last reload:&#32;<s:date name="#lastReloadInfoVar.date" format="dd/MM/yyyy HH:mm" /></span>
            </s:elseif>
        </s:else>
        </s:if>
    </div>
</h3>
<div class="mt-20">
    <table class="table table-striped table-bordered table-hover no-mb">
        <thead>
            <tr>
                <th><s:text name="label.attribute" /></th>
                <th><s:text name="label.attributeType" /></th>
                <th><s:text name="label.expectedConfiguration" /></th>
                <th><s:text name="label.currentConfiguration" /></th>
                <th class="text-center cell-w100"><s:text name="label.status" /></th>
            </tr>
        </thead>
        <tbody>
            <s:iterator var="attributeSettingsVar" value="#contentTypeSettingsVar.attributeSettings">
                <tr>
                    <td><s:property value="#attributeSettingsVar.code" /></td>
                    <td><s:property value="#attributeSettingsVar.typeCode" /></td>
                    <td><s:property value="#attributeSettingsVar.expectedConfig" /></td>
                    <td>
                        <s:iterator var="actualFieldVar" value="#attributeSettingsVar.currentConfig">
                            <s:property value="#actualFieldVar.key" />&#32;&ndash;&#32;<s:property value="#actualFieldVar.value" /><br />
                        </s:iterator>
                    </td>
                    <td class="text-center">
                        <s:if test="#attributeSettingsVar.valid" >
                            <span class="fa fa-circle green" aria-hidden="true" title="OK"></span>
                        </s:if>
                        <s:else>
                            <span class="fa fa-circle red" aria-hidden="true" title="NO"></span>
                        </s:else>
                    </td>
                </tr>
            </s:iterator>
        </tbody>
    </table>
</div>
<hr />
</s:iterator>
