'<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>


<@s.set var="invioMsgKoVar"><@wp.currentWidget param="config" configParam="invioMsgKo"/></@s.set>

<div class="d-flex justify-content-center align-items-center w-100 h-100">
    <i class="fa fa-times-circle text-danger fa-5x" aria-hidden="true"></i>
    <h2><@s.property value="#invioMsgKoVar" /></h2>
</div>
