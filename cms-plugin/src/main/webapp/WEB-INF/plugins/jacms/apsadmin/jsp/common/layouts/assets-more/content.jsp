<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib uri="/apsadmin-core" prefix="wpsa" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%-- tabs --%>
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/jquery.entando.js"></script>
<script>
	jQuery(function(){
		<%--
		when the dom is ready we try to read the hash
		and recognize what is the tab to open.
		--%>
		if (document.location.hash.length > 0) {
			var tabContainer = $('#tab-container'); //tab conainer
			var tabTogglersContainer = $('#tab-togglers'); //toggler container
			//find the parent with class .tab-pane inside the tab container (and get its id)
			var tabId = $($(document.location.hash).parent('.tab-pane'), tabContainer).first().attr('id');
			if (tabId == undefined) {
				var tabId = $(document.location.hash+'.tab-pane', tabContainer).first().attr('id');
			}
			if (tabId!==undefined) {
				//find the element with [href][data-toggle] matching our hash
				var toggler = $('[href="#'+ tabId +'"][data-toggle="tab"]', tabTogglersContainer).first();
				if (toggler.length==1) {
					$(toggler).tab('show');
					// restore scroll position triggering hash change
					var targetElement = document.location.hash;
					location.href = "#";
					location.href = targetElement;
				}
			}
		}
		//enable the clickable togglers (as required by bootstrap)
		$('.tab-togglers a').click(function (e) {
			e.preventDefault()
			$(this).tab('show')
		});
	})
</script>
<%-- tabs //end --%>
<s:include value="/WEB-INF/apsadmin/jsp/common/layouts/assets-common.jsp" />
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/bootstrap-swapon.js"></script>
<s:if test="htmlEditorCode == 'fckeditor'">
	<script type="text/javascript" src="<wp:resourceURL ignoreTenant="true" />administration/js/ckeditor/ckeditor.js"></script>
	<script type="text/javascript" src="<wp:resourceURL ignoreTenant="true" />administration/js/ckeditor/adapters/jquery.js"></script>
</s:if>
<script>
//one domready to rule 'em all
$(function() {
	var emtpyText= '<s:property value="%{getText('note.provide.description')}" escapeJavaScript="true" escapeHtml="false" escapeXml="false" />'

	$('[data-toggle="popover"]').popover();

	/* contentDescription routine //start */
	var fillText = $( '[data-swapon-role="text"]' , '#contentDescription-readonly' ).text();
	$( '#contentDescription-confirm' ).on('swapon', function(ev, action){
		if (action == 'hide') { //when description is filled, update the read only text...
			var contentDescriptionReadOnlyEl = $( '#contentDescription-readonly' );
			var newValue = $( '#contentDescription' ).val();
			$('#contentDescription').val(newValue);
			if (newValue.length<=0) { //if text is an empty string show the default text
				newValue = fillText;
				contentDescriptionReadOnlyEl.next( 'span' ).show();
			}
			else { //otherwise go ahead and hide the second message
				contentDescriptionReadOnlyEl.next( 'span' ).hide();
			}
			$('[data-swapon-role="text"]', '#contentDescription-readonly' ).text( newValue );
		}
	})

	$('#contentDescription-input').on('swapon', function(ev, action) {
		if (action == 'show') {
			var newValue = $.trim($( '#contentDescription-readonly' ).text());
			if (newValue==emtpyText) { newValue = ''; }
			$('#contentDescription').val();
			$('#contentDescription').focus();
		}
	})

	/* contentDescription routine //end */

<s:set var="categoryTreeStyleVar" ><wp:info key="systemParam" paramName="treeStyle_category" /></s:set>

//for content categories
<s:if test="#categoryTreeStyleVar == 'classic'">
	$('.table-treegrid').treegrid(null, false);
	$(".treeRow ").on("click", function (event) {
	    $(".treeRow").removeClass("active");
	    $(this).find('.subTreeToggler').prop("checked", true);
	    $(this).addClass("active");
	});
	
	$("#expandAll").click(function() {
	    $('#categoryTree .treeRow').removeClass('hidden');
	    $('#categoryTree .treeRow').removeClass('collapsed');
	    $('#categoryTree .icon.fa-angle-right').removeClass('fa-angle-right').addClass('fa-angle-down');
	});
	
	$("#collapseAll").click(function() {
	    $('#categoryTree .treeRow:not(:first-child)').addClass('hidden');
	    $('#categoryTree .treeRow').addClass('collapsed');
	    $('#categoryTree .icon.fa-angle-down').removeClass('fa-angle-down').addClass('fa-angle-right');
	});
	
	var selectedNode = $(".table-treegrid .subTreeToggler:checked");
	$(selectedNode).closest(".treeRow").addClass("active");
</s:if>
<s:elseif test="%{#categoryTreeStyleVar == 'request'}">
        $('.table-treegrid').treegrid(null, true);
        $('#categoryTree .expand-icon').hide();
</s:elseif>

<s:include value="/WEB-INF/apsadmin/jsp/common/layouts/assets-more/inc/js_trees_context_menu.jsp" />

//Hypertext Attribute
<s:if test="htmlEditorCode != 'none'">
	$('[data-toggle="entando-hypertext"]').ckeditor({
		customConfig : '<wp:resourceURL ignoreTenant="true" />administration/js/ckeditor/entando-ckeditor_config.js',
		EntandoLinkActionPath: "<s:url namespace="/do/jacms/Content/Hypertext" action="entandoInternalLink"><s:param name="contentOnSessionMarker" value="contentOnSessionMarker" /></s:url>",
		language: '<s:property value="locale" />'
	});
</s:if>
//End Hypertext Attribute
}); //End domready
</script>

<s:include value="/WEB-INF/apsadmin/jsp/common/layouts/assets-more/inc/snippet-datepicker.jsp" />

<script type="text/javascript">
	$(function(){
		var gatherData = function(form, ignoreSelector) {
			var myform = form.serialize();
			if (ignoreSelector!==undefined) {
				var ignored = $(ignoreSelector);
				$.each(ignored, function (index, el) {
					var el = $(el);
					var value =  escape(el.attr('name')) + '=' + escape(el.val());
					myform = myform.replace(value, '');
				})
			}
			return myform;
		};
		var form = $('[data-form-type="autosave"]');
		var button = $('[data-button-type="autosave"]').first();
		var method = form.attr('method');
		var action = form.attr('data-autosave-action');
		var strutsAction = button.attr('name');
		var savedDataEl = $('[data-autosave="last-save-time"]');
		var messagesContainerEl = $('[data-autosave="messages_container"]');
		var versionEl = $('[data-autosave="version"]');
		var lastEditorEl = $('[data-autosave="lastEditor"]');
		var ignoredSelector = '[data-autosave="ignore"]';

		var oldData = gatherData(form,ignoredSelector);

		var sendSave = function(forceSave) {
			var delay = 3000;
			var data = gatherData(form,ignoredSelector);
			if (oldData!=data || forceSave===true) {
				//console.log('data changed');
				$.ajax({
					sentData: data,
					url: action,
					cache: false,
					crossoDomain: true,
					beforeSend: function() {
						button.button('loading');
					},
					complete: function(resp, status) {
						if (status == 'success') {
							oldData = this.sentData;
							setTimeout(function(){
								resp = $.parseJSON(resp.responseText);
								var date = new Date();
								var text = (date.getHours()<10 ? '0'+date.getHours():date.getHours())+':'+(date.getMinutes()<10?'0'+date.getMinutes():date.getMinutes())+':'+(date.getSeconds()<10?'0'+date.getSeconds():date.getSeconds());
								if (resp.hasFieldErrors==true) {
									if (resp.fieldErrorsKeys==='descr' && resp.id==null) {
										text = '<s:property value="%{getText('note.autosave.skipped')}" escapeJavaScript="true" escapeHtml="false" escapeXml="false" />';
									}
									else {
										messagesContainerEl.html(resp.fieldErrors);
									}
									savedDataEl.siblings('.icon').addClass('hide');
								}
								else {
									savedDataEl.siblings('.icon').removeClass('hide');
								}
								button.button('reset');
								savedDataEl.parent().removeClass('hide');
								savedDataEl.text(text);
								versionEl.text(resp.version);
								lastEditorEl.html(resp.lastEditor);
							}, 600);
						}
						else {
							button.button('reset');
							savedDataEl.parent().removeClass('hide');
							savedDataEl.text('<s:property value="%{getText('note.autosave.skipped')}" escapeJavaScript="true" escapeHtml="false" escapeXml="false" />');
						}
					},
					error: function(jqXHR, textStatus, errorThrown) {
						window.location.reload(true);
					},
					processData: false,
					data: data,
					type: method,
					dataType: 'json'
				});
			}
			else {
				//console.log('data unchanged');
				delay = 5000;
			}
			setTimeout(sendSave,delay);
		}
		sendSave();
		$('#edit-saveAndContinue').on('click touchstart', function(ev){
			ev.preventDefault();
			sendSave(true);
		});

		/* Sticky Toolbar */
	    $("body").append("<div class='ghost-toolbar'></div>");
	    $(".ghost-toolbar").height($("#sticky-toolbar").height());

	    $(window).resize(function(){
	        $(".ghost-toolbar").height($("#sticky-toolbar").height());
	        setStickyToolbar();
	    });
	});
	
	function setStickyToolbar() {
	    var width = $("#sticky-toolbar").parent().width();
	    $("#sticky-toolbar").css({position: 'fixed', bottom: 0, zIndex: 1020, width: width});
	}
</script>

<wpsa:hookPoint key="jacms.entryContent.extraResources" objectName="hookPointElements_jacms_entryContent_extraResources">
	<s:iterator value="#hookPointElements_jacms_entryContent_extraResources" var="hookPointElement">
		<wpsa:include value="%{#hookPointElement.filePath}"></wpsa:include>
	</s:iterator>
</wpsa:hookPoint>
