<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>


<h1><wp:i18n key="jpwebform_FORM_SEARCH_DETAIL" /></h1>

<section style="margin-top: 1rem">

    <s:set var="formVar" value="%{getForm(id)}" />


    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.id}" />
    </div>

    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.name}" />
    </div>

    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.campagna}" />
    </div>

    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.submitted}" />
    </div>

    <div style="margin-top: 1rem">
        <s:if test="%{#formVar.delivered}">
            <wp:i18n key="FORM_DELIVERED" /> <br/>
        </s:if>
        <s:else>
            <wp:i18n key="FORM_NOT_DELIVERED" /> <br/>
        </s:else>
    </div>


    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.data.etichettaSel1}" />:&nbsp;<s:property value="%{#formVar.data.valore1}" /> </br>
        <s:property value="%{#formVar.data.etichettaSel2}" />:&nbsp;<s:property value="%{#formVar.data.valore2}" /> </br>
        <s:property value="%{#formVar.data.etichettaSel3}" />:&nbsp;<s:property value="%{#formVar.data.valore3}" /> </br>
        <s:property value="%{#formVar.data.etichettaSel4}" />:&nbsp;<s:property value="%{#formVar.data.valore4}" /> </br>
        <s:property value="%{#formVar.data.etichettaSel5}" />:&nbsp;<s:property value="%{#formVar.data.valore5}" /> </br>
    </div>


    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.data.etichetta1}" />:&nbsp;<s:property value="%{#formVar.data.testo1}" /> </br>
        <s:property value="%{#formVar.data.etichetta2}" />:&nbsp;<s:property value="%{#formVar.data.testo2}" /> </br>
        <s:property value="%{#formVar.data.etichetta3}" />:&nbsp;<s:property value="%{#formVar.data.testo3}" /> </br>
        <s:property value="%{#formVar.data.etichetta4}" />:&nbsp;<s:property value="%{#formVar.data.testo4}" /> </br>
        <s:property value="%{#formVar.data.etichetta5}" />:&nbsp;<s:property value="%{#formVar.data.testo5}" /> </br>
    </div>

    <br/>

    <a style="margin-top: 1rem"
            href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/list.action" escapeAmp="false" />"
            title="<wp:i18n key="jpwebform_FORM_LIST" />"
            class="label label-info display-block">
        <wp:i18n key="jpwebform_FORM_LIST" />&#32;<span class="icon-edit icon-white"></span>
    </a>

    &nbsp;&nbsp;

    <a style="margin-top: 1rem"
            href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/trash.action" escapeAmp="false" ><wp:parameter name="id" ><s:property value="id" /></wp:parameter></wp:action>"
            title="<wp:i18n key="jpwebform_FORM_TRASH" />: <s:property value="id" />"
            class="label label-info display-block">
        title="<wp:i18n key="jpwebform_FORM_TRASH" />:<s:property value="id" />&#32;<span class="icon-edit icon-white"></span>
    </a>


</section>
