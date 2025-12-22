<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<ol class="breadcrumb page-tabs-header breadcrumb-position">
    <li><s:text name="menu.configure"/></li>
    <li>
        <a href="<s:url action="reloadChoose" namespace="/do/BaseAdmin" />">
            <s:text name="title.reload.config" />
        </a>
    </li>
    <li class="page-title-container">
        <s:text name="menu.reload.config" />
    </li>
</ol>
<h1 class="page-title-container">
    <div>
        <s:text name="menu.reload.config" />
        <span class="pull-right">
            <button type="button" class="btn btn-link" data-toggle="popover" data-trigger="focus" data-html="true"
                    title=""
                    data-content="<s:text name='page.reloadConfig.help' />"
                    data-placement="left" data-original-title="">
                <i class="fa fa-question-circle-o fa-2x" aria-hidden="true"></i>
            </button>
        </span>
    </div>
</h1>
<div class="text-right">
    <div class="form-group-separator"></div>
</div>
<br>
<br>
