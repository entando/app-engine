<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>



<h1><wp:i18n key="jpwebform_FORM_TRASH_FORM" /></h1>

<section style="margin-top: 1rem">

    <div id="sure" style="margin-top: 1rem">
        <wp:i18n key="form_DELETE_MESSAGE" />:&nbsp;<s:property value="id" />
    </div>


    <div style="margin-top: 1rem">
        <div>
            <a
                    href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/delete.action" escapeAmp="false" ><wp:parameter name="id" ><s:property value="id" /></wp:parameter></wp:action>"
                    title="<wp:i18n key="FORM_DELETE" />:&nbsp;<s:property value="id" />"
                    class="label label-info display-block">
                <wp:i18n key="jpwebform_FORM_DELETE" />:&nbsp;<s:property value="id" /><span class="icon-edit icon-white"></span>
            </a>
        </div>

        <br/>

        <div>
            <a
                    href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/list.action" escapeAmp="false" />"
                    title="<wp:i18n key="FORM_LIST" />"
                    class="label label-info display-block">
                <wp:i18n key="jpwebform_FORM_LIST" />&#32;<span class="icon-edit icon-white"></span>
            </a>
        </div>
    </div>
</section>
