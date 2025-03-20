<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>


<@s.set var="invioMsgOkVar"><@wp.currentWidget param="config" configParam="successMessage"/></@s.set>
<@s.set var="redirectionAddrVar"><@wp.currentWidget param="config" configParam="redirectUrl"/></@s.set>

<div class="d-flex justify-content-center align-items-center w-100 h-100">
    <i class="fa fa-check-circle text-success fa-5x" aria-hidden="true"></i>
    <h2><@s.property value="#invioMsgOkVar" /></h2>


    <a
            href="<@s.property value="#redirectionAddrVar" />"
            title="<@wp.i18n key="jpwebform_OK_REDIRECTION" />"
            class="btn btn-warning btn-small">
        <@wp.i18n key="jpwebform_OK_REDIRECTION" />
    </a>

</div>
