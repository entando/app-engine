<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>


<h1><wp:i18n key="jpwebform_FORM_SEARCH_DETAIL" /></h1>

<s:set var="formVar" value="%{getForm(id)}" />

<s:property value="%{#formVar.id}" /> <br/>
<s:property value="%{#formVar.name}" /> <br/>
<s:property value="%{#formVar.campagna}" /> <br/>
<%--                    <td><s:property value="%{#formVar.data.valore1}" /></td>--%>
<s:property value="%{#formVar.submitted}" /> <br/>

<s:if test="%{#formVar.delivered}">
    <wp:i18n key="FORM_DELIVERED" /> <br/>
</s:if>
<s:else>
    <wp:i18n key="FORM_NOT_DELIVERED" /> <br/>
</s:else>

<br/>
<a
        href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/list.action"><wp:parameter name="id"><s:property value="#formVar.id" /></wp:parameter></wp:action>"
        title="<wp:i18n key="FORM_LIST" />"
        class="label label-info display-block">
    <s:property value="#formVar.id" />&#32;<span class="icon-edit icon-white"></span>
</a>
