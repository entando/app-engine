<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>
<%@ taglib prefix="c" uri="/struts-tags" %>

<h2>TEO</h2></br>


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
        <s:property value="%{#formVar.campaign}" />
    </div>

    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.submitted}" />
    </div>

    <div style="margin-top: 1rem">
        <s:property value="%{#formVar.serial}" />
    </div>

    <div style="margin-top: 1rem">
        <s:if test="%{#formVar.delivered}">
            <wp:i18n key="FORM_DELIVERED" /> <br/>
        </s:if>
        <s:else>
            <wp:i18n key="FORM_NOT_DELIVERED" /> <br/>
        </s:else>
    </div>

    <form action="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/update.action" escapeAmp="false" />" method="post">
        <p class="noscreen">
            <input type="hidden" name="id" value="<s:property value="#formVar.id" />"/>
        </p>


        <s:set var="etichetta1Var" value="#formVar.data.etichettaSel1"/>
        <s:set var="dropdownVar1" value="%{retrieveDropDown(#formVar, 'valoriOpzione1')}"/>
        <s:if test="%{#etichetta1Var != null && !#etichetta1Var.isEmpty()}">
            <div class="row mt-3">
                <div class="form-group col-md-6 col-sm-12">
                    <label for="etichetta1"><s:property value="etichetta1Var"/></label>

                    <wpsf:select list="#dropdownVar1" name="formData.valore1" cssClass="form-control" id="etichetta1" value="#formVar.data.valore1"/>
                    <input type="hidden" name="formData.etichettaSel1" value="<s:property value="#etichetta1Var" />">
                </div>
            </div>
        </s:if>

        <s:set var="etichetta2Var" value="#formVar.data.etichettaSel2"/>
        <s:set var="dropdownVar2" value="%{retrieveDropDown(#formVar, 'valoriOpzione2')}"/>
        <s:if test="%{#etichetta2Var != null && !#etichetta2Var.isEmpty()}">
            <div class="row mt-3">
                <div class="form-group col-md-6 col-sm-22">
                    <label for="etichetta2"><s:property value="etichetta2Var"/></label>

                    <wpsf:select list="#dropdownVar2" name="formData.valore2" cssClass="form-control" id="etichetta2" value="#formVar.data.valore2"/>
                    <input type="hidden" name="formData.etichettaSel2" value="<s:property value="#etichetta2Var" />">
                </div>
            </div>
        </s:if>

        <s:set var="etichetta3Var" value="#formVar.data.etichettaSel3"/>
        <s:set var="dropdownVar3" value="%{retrieveDropDown(#formVar, 'valoriOpzione3')}"/>
        <s:if test="%{#etichetta3Var != null && !#etichetta3Var.isEmpty()}">
            <div class="row mt-3">
                <div class="form-group col-md-6 col-sm-22">
                    <label for="etichetta3"><s:property value="etichetta3Var"/></label>

                    <wpsf:select list="#dropdownVar3" name="formData.valore3" cssClass="form-control" id="etichetta3" value="#formVar.data.valore3"/>
                    <input type="hidden" name="formData.etichettaSel3" value="<s:property value="#etichetta3Var" />">
                </div>
            </div>
        </s:if>

        <s:set var="etichetta4Var" value="#formVar.data.etichettaSel4"/>
        <s:set var="dropdownVar4" value="%{retrieveDropDown(#formVar, 'valoriOpzione4')}"/>
        <s:if test="%{#etichetta4Var != null && !#etichetta4Var.isEmpty()}">
            <div class="row mt-3">
                <div class="form-group col-md-6 col-sm-22">
                    <label for="etichetta4"><s:property value="etichetta4Var"/></label>

                    <wpsf:select list="#dropdownVar4" name="formData.valore4" cssClass="form-control" id="etichetta4" value="#formVar.data.valore4"/>
                    <input type="hidden" name="formData.etichettaSel4" value="<s:property value="#etichetta4Var" />">
                </div>
            </div>
        </s:if>

        <s:set var="etichetta5Var" value="#formVar.data.etichettaSel5"/>
        <s:set var="dropdownVar5" value="%{retrieveDropDown(#formVar, 'valoriOpzione5')}"/>
        <s:if test="%{#etichetta5Var != null && !#etichetta5Var.isEmpty()}">
            <div class="row mt-3">
                <div class="form-group col-md-6 col-sm-22">
                    <label for="etichetta5"><s:property value="etichetta5Var"/></label>

                    <wpsf:select list="#dropdownVar5" name="formData.valore5" cssClass="form-control" id="etichetta5" value="#formVar.data.valore5"/>
                    <input type="hidden" name="formData.etichettaSel5" value="<s:property value="#etichetta5Var" />">
                </div>
            </div>
        </s:if>


        <div style="margin-top: 1rem">

            <s:if test="#formVar.data.etichetta1 != null && !#formVar.data.etichetta1.isEmpty()">
                <s:property value="%{#formVar.data.etichetta1}" />:&nbsp;<wpsf:textarea name="formData.testo1" cssClass="form-control" id="testo1" value="%{#formVar.data.testo1}" /> </br>
                <input type="hidden" name="formData.etichetta1" value="<s:property value="#formVar.data.etichetta1" />">
            </s:if>

            <s:if test="#formVar.data.etichetta2 != null && !#formVar.data.etichetta2.isEmpty()">
                <s:property value="%{#formVar.data.etichetta2}" />:&nbsp;<wpsf:textarea name="formData.testo2" cssClass="form-control" id="testo2" value="%{#formVar.data.testo2}" /> </br>
                <input type="hidden" name="formData.etichetta2" value="<s:property value="#formVar.data.etichetta2" />">
            </s:if>

            <s:if test="#formVar.data.etichetta3 != null && !#formVar.data.etichetta3.isEmpty()">
                <s:property value="%{#formVar.data.etichetta3}" />:&nbsp;<wpsf:textarea name="formData.testo3" cssClass="form-control" id="testo3" value="%{#formVar.data.testo3}" /> </br>
                <input type="hidden" name="formData.etichetta3" value="<s:property value="#formVar.data.etichetta3" />">
            </s:if>

            <s:if test="#formVar.data.etichetta4 != null && !#formVar.data.etichetta4.isEmpty()">
                <s:property value="%{#formVar.data.etichetta4}" />:&nbsp;<wpsf:textarea name="formData.testo4" cssClass="form-control" id="testo4" value="%{#formVar.data.testo4}" /> </br>
                <input type="hidden" name="formData.etichetta4" value="<s:property value="#formVar.data.etichetta4" />">
            </s:if>

            <s:if test="#formVar.data.etichetta5 != null && !#formVar.data.etichetta5.isEmpty()">
                <s:property value="%{#formVar.data.etichetta5}" />:&nbsp;<wpsf:textarea name="formData.testo5" cssClass="form-control" id="testo5" value="%{#formVar.data.testo5}" /> </br>
                <input type="hidden" name="formData.etichetta5" value="<s:property value="#formVar.data.etichetta5" />">
            </s:if>

        </div>

        <br/>

        <p>
            <s:set var="labelSubmit"><wp:i18n key="Invia il tuo contributo"/></s:set>
            <wpsf:submit useTabindexAutoIncrement="true" value="%{#labelSubmit}" cssClass="btn btn-success"/>
        </p>

    </form>

    <a style="margin-top: 1rem"
       href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/list.action" escapeAmp="false" />"
       title="<wp:i18n key="jpwebform_FORM_LIST" />"
       class="label label-info display-block">
        <wp:i18n key="jpwebform_FORM_LIST" />&#32;<span class="icon-edit icon-white"></span>
    </a>

    </br>

    <wp:pageWithWidget var="pgVar" widgetTypeCode="form_follow_up" />
    <a style="margin-top: 1rem"
       href="<wp:url escapeAmp="false" page="${pgVar.code}"/>?formId=<s:property value="%{#formVar.id}" />"
       title="<wp:i18n key="jpwebform_FORM_TRASH" />: <s:property value="id" />"
       class="label label-info display-block">
        <wp:i18n key="jpwebform_FORM_FOLLOW_UP" />:<s:property value="id" />&#32;<span class="icon-edit icon-white"></span>
    </a>

    </br>

    <a style="margin-top: 1rem"
       href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/trash.action" escapeAmp="false" ><wp:parameter name="id" ><s:property value="id" /></wp:parameter></wp:action>"
       title="<wp:i18n key="jpwebform_FORM_TRASH" />: <s:property value="id" />"
       class="label label-info display-block">
        <wp:i18n key="jpwebform_FORM_TRASH" />:<s:property value="id" />&#32;<span class="icon-edit icon-white"></span>
    </a>


</section>
