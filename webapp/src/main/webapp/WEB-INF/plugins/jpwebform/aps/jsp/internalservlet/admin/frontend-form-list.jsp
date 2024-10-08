<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>

<wp:info key="currentLang" var="currentLang" />

<c:set var="js_for_datepicker">
    /* Italian initialisation for the jQuery UI date picker plugin. */
    /* Written by Antonello Pasella (antonello.pasella@gmail.com). */
    jQuery(function($){
    $.datepicker.regional['it'] = {
    closeText: 'Chiudi',
    prevText: '&#x3c;Prec',
    nextText: 'Succ&#x3e;',
    currentText: 'Oggi',
    monthNames: ['Gennaio','Febbraio','Marzo','Aprile','Maggio','Giugno',
    'Luglio','Agosto','Settembre','Ottobre','Novembre','Dicembre'],
    monthNamesShort: ['Gen','Feb','Mar','Apr','Mag','Giu',
    'Lug','Ago','Set','Ott','Nov','Dic'],
    dayNames: ['Domenica','Luned&#236','Marted&#236','Mercoled&#236','Gioved&#236','Venerd&#236','Sabato'],
    dayNamesShort: ['Dom','Lun','Mar','Mer','Gio','Ven','Sab'],
    dayNamesMin: ['Do','Lu','Ma','Me','Gi','Ve','Sa'],
    weekHeader: 'Sm',
    dateFormat: 'dd/mm/yy',
    firstDay: 1,
    isRTL: false,
    showMonthAfterYear: false,
    yearSuffix: ''};
    });

    jQuery(function($){
    if (Modernizr.touch && Modernizr.inputtypes.date) {
    $.each( $("input[data-isdate=true]"), function(index, item) {
    item.type = 'date';
    });
    } else {
    $.datepicker.setDefaults( $.datepicker.regional[ "<c:out value="${currentLang}" />" ] );
    $("input[data-isdate=true]").datepicker({
    changeMonth: true,
    changeYear: true,
    dateFormat: "dd/mm/yy"
    });
    }
    });
</c:set>


<section class="form_list">

    <h1><wp:i18n key="jpwebform_FORM_SEARCH_FORM" /></h1>

    <form action="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/list.action" />" method="post" >

        <fieldset>
            <label for="form_id"><wp:i18n key="jpwebform_FORM_ID" /></label>
            <input type="text" name="id" id="form_id" value="<s:property value="id" />" />   <br/>
            <label for="form_name"><wp:i18n key="jpwebform_FORM_NAME" /></label>
            <input type="text" name="name" id="form_name" value="<s:property value="name" />" />   <br/>
            <label for="form_campaign"><wp:i18n key="jpwebform_FORM_CAMPAIGN" /></label>
            <input type="text" name="campagna" id="form_campaign" value="<s:property value="campagna" />" />   <br/>
            <label for="form_submittedStart_cal"><wp:i18n key="jpwebform_FORM_SUBMITTEDSTART" /></label>
            <input type="text" name="submittedStart" id="form_submittedStart_cal" data-isdate="true" value="<s:property value="from" />" />   <br/>
            <label for="form_submittedEnd_cal"><wp:i18n key="jpwebform_FORM_SUBMITTEDEND" /></label>
            <input type="text" name="submittedEnd" id="form_submittedEnd_cal" data-isdate="true" value="<s:property value="to" />" />   <br/>
            <label for="form_delivered"><wp:i18n key="jpwebform_FORM_DELIVERED" /></label>
            <wpsf:select name="delivered" id="form_delivered" list="{'--', 'true', 'false'}" value="%{delivered}" />
        </fieldset>

        <button type="submit" class="btn btn-primary">
            <wp:i18n key="SEARCH" />
        </button>

        <wpsa:subset source="formsId" count="10" objectName="groupForm" advanced="true" offset="5">
        <s:set var="group" value="#groupForm" />

        <div class="margin-medium-vertical text-center">
            <s:include value="/WEB-INF/apsadmin/jsp/common/inc/pagerInfo.jsp" />
            <s:include value="/WEB-INF/apsadmin/jsp/common/inc/pager_formBlock.jsp" />
        </div>


        <table class="table table-bordered table-condensed table-striped">
            <thead>
            <tr>
                <th
                        class="text-right"><wp:i18n key="jpwebform_FORM_ID" /></th>
                <th
                        class="text-left"><wp:i18n key="jpwebform_FORM_NAME" /></th>
                <th
                        class="text-center"><wp:i18n key="jpwebform_FORM_CAMPAGNA" /></th>
<%--                <th--%>
<%--                        class="text-left"><wp:i18n key="jpwebform_FORM_DATA" /></th>--%>
                <th
                        class="text-left"><wp:i18n key="jpwebform_FORM_SUBMITTED" /></th>
                <th
                        class="text-left"><wp:i18n key="jpwebform_FORM_DELIVERED" /></th>
                <th>
                    <wp:i18n key="jpwebform_FORM_ACTIONS" /> </th>
            </tr>
            </thead>
            <tbody>
            <s:iterator var="formIdVar">
                <s:set var="formVar" value="%{getForm(#formIdVar)}" />
                <tr>
                    <td>
                        <a
                                href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/formDetail.action" escapeAmp="false"><wp:parameter name="id"><s:property value="#formVar.id" /></wp:parameter></wp:action>"
                                title="<wp:i18n key="DETAIL" />: <s:property value="#formVar.id" />"
                                class="label label-info display-block">
                            <s:property value="#formVar.id" />&#32;
                            <span class="icon-edit icon-white"></span>
                        </a>
                    </td>
                    <td><s:property value="%{#formVar.name}" /></td>
                    <td><s:property value="%{#formVar.campagna}" /></td>
<%--                    <td><s:property value="%{#formVar.data.valore1}" /></td>--%>
                    <td><s:property value="%{#formVar.submitted}" /></td>
                    <td>
                        <s:if test="%{#formVar.delivered}">
                            <wp:i18n key="FORM_DELIVERED" />
                        </s:if>
                        <s:else>
                            <wp:i18n key="FORM_NOT_DELIVERED" />
                        </s:else>
                    </td>

                    <td class="text-center">
                        <a
                                href="<wp:action path="/ExtStr2/do/FrontEnd/jpwebform/Form/trash.action" escapeAmp="false"><wp:parameter name="id"><s:property value="#formVar.id" /></wp:parameter></wp:action>"
                                title="<wp:i18n key="TRASH" />: <s:property value="#formVar.id" />"
                                class="btn btn-warning btn-small">
                            <span class="icon-trash icon-white"></span>&#32;
                            <wp:i18n key="TRASH" />
                        </a>
                    </td>

                </tr>
            </s:iterator>
            </tbody>

            <div class="margin-medium-vertical text-center">
                <s:include value="/WEB-INF/apsadmin/jsp/common/inc/pager_formBlock.jsp" />
            </div>

            </wpsa:subset>

    </form>
</section>
