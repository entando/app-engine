<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>


<@s.set var="invioMsgOkVar"><@wp.currentWidget param="config" configParam="invioMsgOk"/></@s.set>

<div class="d-flex justify-content-center align-items-center w-100 h-100">
    <i class="fa fa-check-circle text-success fa-5x" aria-hidden="true"></i>
    <h2><@s.property value="#invioMsgOkVar" /></h2>

</div>
