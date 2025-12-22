<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<s:include value="/WEB-INF/apsadmin/jsp/admin/inc/reload-menu-intro.jsp" />

<div>
    <div class="col-12 alert alert-warning margin-large-top text-left">
        <span class="pficon pficon-warning-triangle-o"></span>
        <strong><s:text name="messages.warning" /></strong>: <s:text name="reload.legend.info" />
    </div>
</div>

<br/>

<s:if test="1 == reloadingResult || 3 == reloadingResult">

    <s:if test="1 == reloadingResult">
        <div class="alert alert-success">
            <span class="pficon pficon-ok"></span>
            <strong><s:text name="messages.confirm" /></strong>!&#32;
            <s:text name="message.reloadConfig.ok" />.
        </div>
    </s:if>
    <s:else>
        <div class="alert alert-warning">
            <span class="pficon pficon-warning-triangle-o"></span>
            <strong><s:text name="messages.warning" /></strong>!&#32;
            <s:text name="message.reloadConfig.warning" />.
        </div>
    </s:else>
    <table class="table table-bordered table-striped table-hover table-condensed">
        <thead>
        <tr>
            <th>#</th>
            <th><s:text name="reload.table.head.beanId"/></th>
            <th><s:text name="reload.table.head.status"/></th>
        </tr>
        </thead>
        <tbody>
        <s:iterator value="reloadInfo.entrySet()" var="beanVar" status="rowVar">
            <tr>
                <td><s:property value="#rowVar.index + 1" /></td>
                <s:if test="#beanVar.value == null || #beanVar.value.trim().length() == 0">
                    <td><s:property value="#beanVar.key" /></td>
                    <td><s:text name="reload.bean.status.ok" /></td>
                </s:if>
                <s:else>
                    <td><b><s:property value="#beanVar.key" /></b></td>
                    <td><s:text name="reload.bean.status.ko" />:&nbsp;<s:property value="#beanVar.value" /></td>
                </s:else>
            </tr>
        </s:iterator>
        </tbody>
    </table>
</s:if>
<s:elseif test="0 == reloadingResult">
    <div class="alert alert-danger alert-dismissable">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">
            <span class="pficon pficon-close"></span>
        </button>
        <span class="pficon pficon-error-circle-o"></span>
        <strong><s:text name="messages.error" /></strong>!&#32;
        <s:text name="message.reloadConfig.ko" />.
    </div>
</s:elseif>
