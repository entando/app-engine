<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>

<h1 class="panel panel-default title-page">
	<span class="panel-body display-block">
		<a href="<s:url action="viewTree" namespace="/do/Page" />" title="<s:text name="note.goToSomewhere" />: <s:text name="title.pageManagement" />">
			<s:text name="title.pageManagement" /></a>&#32;/&#32;
		<a href="<s:url action="configure" namespace="/do/Page"><s:param name="pageCode"><s:property value="currentPage.code"/></s:param></s:url>" title="<s:text name="note.goToSomewhere" />: <s:text name="title.configPage" />"><s:text name="title.configPage" /></a>&#32;/&#32;
		<s:text name="name.widget" />
	</span>
</h1>

<div id="main" role="main">

	<s:set var="breadcrumbs_pivotPageCode" value="pageCode" />
	<s:include value="/WEB-INF/apsadmin/jsp/portal/include/pageInfo_breadcrumbs.jsp" />

	<s:action namespace="/do/Page" name="printPageDetails" executeResult="true" ignoreContextParams="true"><s:param name="selectedNode" value="pageCode"></s:param></s:action>

	<s:form action="saveConfig" namespace="/do/jpwebform/Form/Page/SpecialWidget/jpwebformFormConfig" cssClass="form-horizontal">
		<div class="panel panel-default">
			<div class="panel-heading">
				<s:include value="/WEB-INF/apsadmin/jsp/portal/include/frameInfo.jsp" />
			</div>

			<div class="panel-body">

				<h2 class="h5 margin-small-vertical">
					<label class="sr-only"><s:text name="name.widget" /></label>
					<span class="icon fa fa-puzzle-piece" title="<s:text name="name.widget" />"></span>&#32;
					<s:property value="%{getTitle(showlet.type.code, showlet.type.titles)}" />
				</h2>

				<p class="sr-only">
					<wpsf:hidden name="pageCode" />
					<wpsf:hidden name="frame" />
					<wpsf:hidden name="widgetTypeCode" value="%{widget.type.code}" />
				</p>

				<s:if test="hasFieldErrors()">
					<div class="alert alert-danger alert-dismissable">
						<button class="close" data-dismiss="alert"><span class="icon fa fa-times"></span></button>
						<h3 class="h4 margin-none"><s:text name="message.title.FieldErrors" /></h3>
						<ul>
							<s:iterator value="fieldErrors">
								<s:iterator value="value">
									<li><s:property escapeHtml="false" /></li>
								</s:iterator>
							</s:iterator>
						</ul>
					</div>
				</s:if>

				<div class="form-horizontal">

					<fieldset class="no-padding">
						<legend role="heading">Generali</legend>
						<div class="no-padding col-lg-12 col-md-12">

							<div class="form-group">
								<label class="col-md-1 control-label" for="titolo">Titolo</label>
								<div class="col-md-6">
									<wpsf:textfield name="titolo" cssClass="form-control" id="titolo"/>
								</div>
							</div>
							<div class="form-group">
								<label class="col-md-1 control-label" for="description">Descrizione</label>
								<div class="col-md-6">
									<wpsf:textfield name="descrizione" cssClass="form-control" id="description"/>
								</div>
							</div>

							<div class="form-group">
								<label class="col-md-1 control-label" for="idDestinatario">ID Destinatario</label>
								<div class="col-md-6">
									<wpsf:textfield name="idDestinatario" cssClass="form-control" id="idDestinatario"/>
								</div>
							</div>

							<div class="form-group">
								<label class="col-md-1 control-label" for="subject">Oggetto Email</label>
								<div class="col-md-6">
									<wpsf:textfield name="oggetto" cssClass="form-control" id="subject"/>
								</div>
							</div>

							<div class="form-group">
								<label class="col-md-1 control-label" for="deliverOk">Messaggio di conferma invio</label>
								<div class="col-md-6">
									<wpsf:textfield name="invioMsgOk" cssClass="form-control" id="deliverOk"/>
								</div>
							</div>

							<div class="form-group">
								<label class="col-md-1 control-label" for="deliverKo">Messaggio di errore invio</label>
								<div class="col-md-6">
									<wpsf:textfield name="invioMsgKo" cssClass="form-control" id="deliverKo"/>
								</div>
							</div>

						</div>
					</fieldset>

					<fieldset class="no-padding">
						<legend role="heading">Dropdown</legend>
						<div class="no-padding col-lg-12 col-md-12">

								<%--DROPDOWN #1--%>
							<div class="form-group">
								<label class="col-md-1 control-label" for="select1">Etichetta opzione 1</label>
								<div class="col-md-6">
									<wpsf:textfield name="opzione1" cssClass="form-control" id="select1"/>
								</div>
							</div>
							<div class="form-group">
								<label class="col-md-1 control-label" for="valoriOpzione1">Valori opzione 1 (valore1;valore2;...)</label>
								<div class="col-md-6">
									<wpsf:textfield name="valoriOpzione1" cssClass="form-control" id="valoriOpzione1"/>
								</div>
							</div>

								<%--DROPDOWN #2--%>
							<div class="form-group">
								<label class="col-md-1 control-label" for="select2">Etichetta opzione 2</label>
								<div class="col-md-6">
									<wpsf:textfield name="opzione2" cssClass="form-control" id="select2"/>
								</div>
							</div>
							<div class="form-group">
								<label class="col-md-1 control-label" for="valoriOpzione2">Valori opzione 2 (valore1;valore2;...)</label>
								<div class="col-md-6">
									<wpsf:textfield name="valoriOpzione2" cssClass="form-control" id="valoriOpzione2"/>
								</div>
							</div>


								<%--DROPDOWN #3--%>
							<div class="form-group">
								<label class="col-md-1 control-label" for="select3">Etichetta opzione 3</label>
								<div class="col-md-6">
									<wpsf:textfield name="opzione3" cssClass="form-control" id="select3"/>
								</div>
							</div>
							<div class="form-group">
								<label class="col-md-1 control-label" for="valoriOpzione3">Valori opzione 3 (valore1;valore2;...)</label>
								<div class="col-md-6">
									<wpsf:textfield name="valoriOpzione3" cssClass="form-control" id="valoriOpzione3"/>
								</div>
							</div>

								<%--DROPDOWN #4--%>
							<div class="form-group">
								<label class="col-md-1 control-label" for="select4">Etichetta opzione 4</label>
								<div class="col-md-6">
									<wpsf:textfield name="opzione4" cssClass="form-control" id="select4"/>
								</div>
							</div>
							<div class="form-group">
								<label class="col-md-1 control-label" for="valoriOpzione4">Valori opzione 4 (valore1;valore2;...)</label>
								<div class="col-md-6">
									<wpsf:textfield name="valoriOpzione4" cssClass="form-control" id="valoriOpzione4"/>
								</div>
							</div>

								<%--DROPDOWN #5 --%>
							<div class="form-group">
								<label class="col-md-1 control-label" for="select5">Etichetta opzione 5</label>
								<div class="col-md-6">
									<wpsf:textfield name="opzione5" cssClass="form-control" id="select5"/>
								</div>
							</div>
							<div class="form-group">
								<label class="col-md-1 control-label" for="valoriOpzione5">Valori opzione 5 (valore1;valore2;...)</label>
								<div class="col-md-6">
									<wpsf:textfield name="valoriOpzione5" cssClass="form-control" id="valoriOpzione5"/>
								</div>
							</div>

						</div>
					</fieldset>

					<fieldset class="no-padding">
						<legend role="heading">Dropdown</legend>
						<div class="no-padding col-lg-12 col-md-12">

								<%-- TEXT #1 --%>
							<div class="form-group">
								<div class="row">
									<label class="col-md-1 control-label" for="etichetta1">Etichetta campo di testo 1</label>
									<div class="col-md-6">
										<wpsf:textfield name="etichetta1" cssClass="form-control" id="etichetta1"/>
									</div>
								</div>
								<div class="row">
									<label class="col-md-1 control-label" for="obbligatorio1">
										Campo di testo 1 obbligatorio
									</label>
									<div class="col-md-6">
										<wpsf:checkbox name="obbligatorio1"  id="obbligatorio1"/>
									</div>
								</div>
							</div>
							<hr class="divider"/>


								<%-- TEXT #2 --%>
							<div class="form-group">
								<div class="row">
									<label class="col-md-1 control-label" for="etichetta2">Etichetta campo di testo 2</label>
									<div class="col-md-6">
										<wpsf:textfield name="etichetta2" cssClass="form-control" id="etichetta2"/>
									</div>
								</div>
								<div class="row">
									<label class="col-md-1 control-label" for="obbligatorio2">
										Campo di testo 2 obbligatorio
									</label>
									<div class="col-md-6">
										<wpsf:checkbox name="obbligatorio2"  id="obbligatorio2"/>
									</div>
								</div>
							</div>
							<hr class="divider"/>

								<%-- TEXT #3 --%>
							<div class="form-group">
								<div class="row">
									<label class="col-md-1 control-label" for="etichetta3">Etichetta campo di testo 3</label>
									<div class="col-md-6">
										<wpsf:textfield name="etichetta3" cssClass="form-control" id="etichetta3"/>
									</div>
								</div>
								<div class="row">
									<label class="col-md-1 control-label" for="obbligatorio3">
										Campo di testo 3 obbligatorio
									</label>
									<div class="col-md-6">
										<wpsf:checkbox name="obbligatorio3"  id="obbligatorio3"/>
									</div>
								</div>
							</div>
							<hr class="divider"/>

								<%-- TEXT #4 --%>
							<div class="form-group">
								<div class="row">
									<label class="col-md-1 control-label" for="etichetta4">Etichetta campo di testo 4</label>
									<div class="col-md-6">
										<wpsf:textfield name="etichetta4" cssClass="form-control" id="etichetta4"/>
									</div>
								</div>
								<div class="row">
									<label class="col-md-1 control-label" for="obbligatorio4">
										Campo di testo 4 obbligatorio
									</label>
									<div class="col-md-6">
										<wpsf:checkbox name="obbligatorio4"  id="obbligatorio4"/>
									</div>
								</div>
							</div>
							<hr class="divider"/>


								<%-- TEXT #5 --%>
							<div class="form-group">
								<div class="row">
									<label class="col-md-1 control-label" for="etichetta5">Etichetta campo di testo 5</label>
									<div class="col-md-6">
										<wpsf:textfield name="etichetta5" cssClass="form-control" id="etichetta5"/>
									</div>
								</div>
								<div class="row">
									<label class="col-md-1 control-label" for="obbligatorio5">
										Campo di testo 5 obbligatorio
									</label>
									<div class="col-md-6">
										<wpsf:checkbox name="obbligatorio5"  id="obbligatorio5"/>
									</div>
								</div>
							</div>

						</div>
					</fieldset>

					<span class="input-group-btn">
						<wpsf:submit type="button" cssClass="btn btn-success">
							<span class="icon fa fa-check"></span>&#32;
							<s:text name="label.confirm" />
						</wpsf:submit>
					</span>
				</div>
			</div>
		</div>

	</s:form>
</div>
