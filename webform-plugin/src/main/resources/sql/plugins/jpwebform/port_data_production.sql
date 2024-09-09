INSERT INTO widgetcatalog (code, titles, parameters, plugincode, parenttypecode, defaultconfig, locked, maingroup) VALUES ('jpwebformForm', '<?xml version="1.0" encoding="UTF-8"?>
<properties>
<property key="en">Publish Form</property>
<property key="it">Pubblica Form</property>
</properties>', '<config>
	<parameter name="titolo">titolo descr</parameter>
	<parameter name="descrizione">descrizione</parameter>
	<parameter name="idDestinatario">idDestinatario</parameter>
	<parameter name="oggetto">oggetto</parameter>
	<parameter name="invioMsgOk">invioMsgOk</parameter>
	<parameter name="invioMsgKo">invioMsgKo</parameter>
	<parameter name="opzione1">opzione1</parameter>
	<parameter name="valoriOpzione1">valoriOpzione1</parameter>
	<parameter name="opzione2">opzione2</parameter>
	<parameter name="valoriOpzione2">valoriOpzione2</parameter>
	<parameter name="opzione3">opzione3</parameter>
	<parameter name="valoriOpzione3">valoriOpzione3</parameter>
	<parameter name="opzione4">opzione4</parameter>
	<parameter name="valoriOpzione4">valoriOpzione4</parameter>
	<parameter name="opzione5">opzione5</parameter>
	<parameter name="valoriOpzione5">valoriOpzione5</parameter>
	<parameter name="etichetta1">etichetta1</parameter>
	<parameter name="obbligatorio1">obbligatorio1</parameter>
	<parameter name="etichetta2">etichetta2</parameter>
	<parameter name="obbligatorio2">obbligatorio2</parameter>
	<parameter name="etichetta3">etichetta3</parameter>
	<parameter name="obbligatorio3">obbligatorio3</parameter>
	<parameter name="etichetta4">etichetta4</parameter>
	<parameter name="obbligatorio4">obbligatorio4</parameter>
	<parameter name="etichetta5">etichetta5</parameter>
	<parameter name="obbligatorio5">obbligatorio5</parameter>
	<action name="jpwebformFormConfig"/>
</config>','jpwebform', NULL, NULL, 1, 'free');



INSERT INTO guifragment (code, widgettypecode, plugincode, gui, defaultgui, locked) VALUES ('jpwebformForm_is_front_Form_error', NULL, 'jpwebform', NULL, '<pre>
   __   _ __   _ __   ___   _ __
 /''__`\/\`''__\/\`''__\/ __`\/\`''__\
/\  __/\ \ \/ \ \ \//\ \L\ \ \ \/
\ \____\\ \_\  \ \_\\ \____/\ \_\
 \/____/ \/_/   \/_/ \/___/  \/_/

</pre>', 1);


INSERT INTO guifragment (code, widgettypecode, plugincode, gui, defaultgui, locked) VALUES ('jpwebform_form_render', NULL, 'jpwebform', NULL, '<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsa=JspTaglibs["/apsadmin-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>


<div class="container">

    <@s.set var="titoloVar"><@wp.currentWidget param="config" configParam="titolo"/></@s.set>
    <@s.set var="descrizioneVar"><@wp.currentWidget param="config" configParam="descrizione"/></@s.set>
    <@s.set var="idDestinatarioVar"><@wp.currentWidget param="config" configParam="idDestinatario"/></@s.set>


    <div class="row mt-3">
        <div class="col-12">
            <h1 class="h1 text-center"><@s.property value="#titoloVar"/></h1>
        </div>
        <div class="col-12">
            <p class="lead text-center"><@s.property value="#descrizioneVar"/></p>
        </div>


        <@s.if test="hasFieldErrors()">
            <div class="alert alert-error">
                <h2><@s.text name="message.title.FieldErrors" /></h2>
                <ul>
                    <@s.iterator value="fieldErrors">
                        <@s.iterator value="value">
                            <li><@s.property /></li>
                        </@s.iterator>
                    </@s.iterator>
                </ul>
            </div>
        </@s.if>
        <@s.if test="hasActionErrors()">
            <div class="alert alert-error">
                <h2><@s.text name="message.title.ActionErrors" /></h2>
                <ul>
                    <@s.iterator value="actionErrors">
                        <li><@s.property /></li>
                    </@s.iterator>
                </ul>
            </div>
        </@s.if>


        <form action="<@wp.action path="/ExtStr2/do/FrontEnd/jpwebform/Form/deliver.action"/>" method="post">
            <p class="noscreen">
                <input type="hidden" name="idDestinatario" value="<@s.property value="#idDestinatarioVar" />"/>
                <input type="hidden" name="pageCode" value="<@wp.currentPage param="code"/>"/>
            </p>

            <#-- RENDERIZZAZIONE DROPDOWN -->

            <@s.set var="valorietichetta1Var"><@wp.currentWidget param="config" configParam="valoriOpzione1"/></@s.set>
            <@s.set var="dropdown1" value="%{generateDropDown(#valorietichetta1Var)}"/>
            <@s.set var="etichetta1Var"><@wp.currentWidget param="config" configParam="opzione1"/></@s.set>
            <@s.if test="%{#etichetta1Var != null && !#etichetta1Var.isEmpty() && #valorietichetta1Var != null && !#valorietichetta1Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="etichetta1"><@s.property value="etichetta1Var"/></label>

                        <@wpsf.select list="#dropdown1" name="formData.valore1" cssClass="form-control" id="etichetta1"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="valorietichetta2Var"><@wp.currentWidget param="config" configParam="valoriOpzione2"/></@s.set>
            <@s.set var="dropdown2" value="%{generateDropDown(#valorietichetta2Var)}"/>
            <@s.set var="etichetta2Var"><@wp.currentWidget param="config" configParam="opzione2"/></@s.set>
            <@s.if test="%{#etichetta2Var != null && !#etichetta2Var.isEmpty() && #valorietichetta2Var != null && !#valorietichetta2Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="etichetta2"><@s.property value="etichetta2Var"/></label>

                        <@wpsf.select list="#dropdown2" name="formData.valore2" cssClass="form-control" id="etichetta2"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="valorietichetta3Var"><@wp.currentWidget param="config" configParam="valoriOpzione3"/></@s.set>
            <@s.set var="dropdown3" value="%{generateDropDown(#valorietichetta3Var)}"/>
            <@s.set var="etichetta3Var"><@wp.currentWidget param="config" configParam="opzione3"/></@s.set>
            <@s.if test="%{#etichetta3Var != null && !#etichetta3Var.isEmpty() && #valorietichetta3Var != null && !#valorietichetta3Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="etichetta3"><@s.property value="etichetta3Var"/></label>

                        <@wpsf.select list="#dropdown3" name="formData.valore3" cssClass="form-control" id="etichetta3"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="valorietichetta4Var"><@wp.currentWidget param="config" configParam="valoriOpzione4"/></@s.set>
            <@s.set var="dropdown4" value="%{generateDropDown(#valorietichetta4Var)}"/>
            <@s.set var="etichetta4Var"><@wp.currentWidget param="config" configParam="opzione4"/></@s.set>
            <@s.if test="%{#etichetta4Var != null && !#etichetta4Var.isEmpty() && #valorietichetta4Var != null && !#valorietichetta4Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="etichetta4"><@s.property value="etichetta4Var"/></label>

                        <@wpsf.select list="#dropdown4" name="formData.valore4" cssClass="form-control" id="etichetta4"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="valorietichetta5Var"><@wp.currentWidget param="config" configParam="valoriOpzione5"/></@s.set>
            <@s.set var="dropdown5" value="%{generateDropDown(#valorietichetta5Var)}"/>
            <@s.set var="etichetta5Var"><@wp.currentWidget param="config" configParam="opzione5"/></@s.set>
            <@s.if test="%{#etichetta5Var != null && !#etichetta5Var.isEmpty() && #valorietichetta5Var != null && !#valorietichetta5Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="etichetta5"><@s.property value="etichetta5Var"/></label>

                        <@wpsf.select list="#dropdown5" name="formData.valore5" cssClass="form-control" id="etichetta5"/>
                    </div>
                </div>
            </@s.if>

            <#-- RENDERIZZAZIONE TEXTFIELD -->
            <@s.set var="etichetta1Var"><@wp.currentWidget param="config" configParam="etichetta1"/></@s.set>
            <@s.if test="%{#etichetta1Var != null && !#etichetta1Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="testo1"><@s.property value="#etichetta1Var"/></label>

                        <@wpsf.textarea name="formData.testo1" cssClass="form-control" id="testo1"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="etichetta2Var"><@wp.currentWidget param="config" configParam="etichetta2"/></@s.set>
            <@s.if test="%{#etichetta2Var != null && !#etichetta2Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="testo2"><@s.property value="#etichetta2Var"/></label>

                        <@wpsf.textarea name="formData.testo2" cssClass="form-control" id="testo2"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="etichetta3Var"><@wp.currentWidget param="config" configParam="etichetta3"/></@s.set>
            <@s.if test="%{#etichetta3Var != null && !#etichetta3Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-13">
                        <label for="testo3"><@s.property value="#etichetta3Var"/></label>

                        <@wpsf.textarea name="formData.testo3" cssClass="form-control" id="testo3"/>
                    </div>
                </div>
            </@s.if>

            <@s.set var="etichetta4Var"><@wp.currentWidget param="config" configParam="etichetta4"/></@s.set>
            <@s.if test="%{#etichetta4Var != null && !#etichetta4Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-13">
                        <label for="testo4"><@s.property value="#etichetta4Var"/></label>

                        <@wpsf.textarea name="formData.testo4" cssClass="form-control" id="testo4"/>
                    </div>
                </div>
            </@s.if>


            <@s.set var="etichetta5Var"><@wp.currentWidget param="config" configParam="etichetta5"/></@s.set>
            <@s.if test="%{#etichetta5Var != null && !#etichetta5Var.isEmpty()}">
                <div class="row mt-3">
                    <div class="form-group col-md-6 col-sm-12">
                        <label for="testo5"><@s.property value="#etichetta5Var"/></label>

                        <@wpsf.textarea name="formData.testo5" cssClass="form-control" id="testo5"/>

                    </div>
                </div>
            </@s.if>

            <p>
                <@wp.i18n key="Invia il tuo contributo" var="labelSubmitVar" />
                <#assign labelSubmitStrVar>
                    <@s.property value=''labelSubmitVar''/>
                </#assign>
                <@wpsf.submit useTabindexAutoIncrement=true value=labelSubmitStrVar cssClass="btn btn-success" />
            </p>
        </form>
    </div>
</div>
', 1);

INSERT INTO guifragment (code, widgettypecode, plugincode, gui, defaultgui, locked) VALUES ('jpwebform_form_ko', NULL, 'jpwebform', NULL, '<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>


<@s.set var="invioMsgKoVar"><@wp.currentWidget param="config" configParam="invioMsgKo"/></@s.set>

<div class="d-flex justify-content-center align-items-center w-100 h-100">
    <i class="fa fa-times-circle text-danger fa-5x" aria-hidden="true"></i>
    <h2><@s.property value="#invioMsgKoVar" /></h2>
</div>', 1);

INSERT INTO guifragment (code, widgettypecode, plugincode, gui, defaultgui, locked) VALUES ('jpwebform_form_ok', NULL, 'jpwebform', NULL, '<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>


<@s.set var="invioMsgOkVar"><@wp.currentWidget param="config" configParam="invioMsgOk"/></@s.set>

<div class="d-flex justify-content-center align-items-center w-100 h-100">
    <i class="fa fa-check-circle text-success fa-5x" aria-hidden="true"></i>
    <h2><@s.property value="#invioMsgOkVar" /></h2>

</div>', 1);
