<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/aps-core" prefix="wp" %>
<%@ taglib uri="/apsadmin-core" prefix="wpsa" %>
<%@ taglib prefix="wpsf" uri="/apsadmin-form" %>

<s:set var="currentLink" value="%{#attribute.symbolicLinks[#lang.code]}" />
<s:set var="defaultLink" value="#attribute.symbolicLink" />
<s:if test="null == #currentLink"><s:set var="linkToEdit" value="#defaultLink" /></s:if><s:else><s:set var="linkToEdit" value="#currentLink" /></s:else>

<span class="sr-only"><s:text name="note.linkContent" /></span>

<wpsa:actionParam action="chooseLink" var="chooseLinkActionName" >
    <wpsa:actionSubParam name="parentAttributeName" value="%{#parentAttribute.name}" />
    <wpsa:actionSubParam name="attributeName" value="%{#attribute.name}" />
    <wpsa:actionSubParam name="elementIndex" value="%{#elementIndex}" />
    <wpsa:actionSubParam name="langCodeOfLink" value="%{#lang.code}" />
</wpsa:actionParam>

<s:if test="#linkToEdit != null">
    <div class="panel panel-default margin-small-top">
</s:if>

<s:if test="#linkToEdit != null">
    <div class="panel-body">
        <div class="col-xs-12 form-horizontal">
            <div class="form-group">
                <s:if test="#linkToEdit.destType == 2 || #linkToEdit.destType == 4">
                    <s:set var="linkedPage" value="%{getPage(#linkToEdit.pageDestination)}" />
                </s:if>
                <s:if test="#linkToEdit.destType == 3 || #linkToEdit.destType == 4">
                    <s:set var="linkedContent" value="%{getContentVo(#linkToEdit.contentDestination)}" />
                </s:if>
                <s:set var="validLink" value="true" />
                <s:if test="(#linkToEdit.destType == 2 || #linkToEdit.destType == 4) && #linkedPage == null">
                    <s:text name="LinkAttribute.fieldError.linkToPage.voidPage" />
                    <s:set var="validLink" value="false" />
                </s:if>
                <s:if test="(#linkToEdit.destType == 3 || #linkToEdit.destType == 4) && (#linkedContent == null || !#linkedContent.onLine)">
                    <s:text name="LinkAttribute.fieldError.linkToContent" />
                    <s:set var="validLink" value="false" />
                </s:if>
                <s:if test="#validLink">
                    <%-- link with a valid value --%>
                    <s:if test="#linkToEdit.destType == 1">
                        <s:set var="statusIconVar">btn btn-default disabled icon fa fa-globe</s:set>
                        <s:set var="linkDestination" value="%{#linkToEdit.urlDest}" />
                        <s:set var="linkType" value="%{getText('note.URLLinkTo')}"/>
                    </s:if>
                    <s:if test="#linkToEdit.destType == 2">
                        <s:set var="statusIconVar">btn btn-default disabled icon fa fa-folder</s:set>
                        <s:set var="linkDestination" value="%{#linkedPage.titles[currentLang.code]}" />
                        <s:set var="linkType" value="%{getText('note.pageLinkTo')}"/>
                    </s:if>
                    <s:if test="#linkToEdit.destType == 3">
                        <s:set var="statusIconVar">btn btn-default disabled icon fa fa-file-text-o</s:set>
                        <s:set var="linkDestination" value="%{#linkToEdit.contentDestination + ' - ' + #linkedContent.descr}" />
                        <s:set var="linkType" value="%{getText('note.contentLinkTo')}"/>
                    </s:if>
                    <s:if test="#linkToEdit.destType == 4">
                        <s:set var="statusIconVar">btn btn-default disabled icon fa fa-file-text</s:set>
                        <s:set var="linkDestination" value="%{#linkToEdit.contentDestination + ' - ' + #linkedContent.descr + ', ' + getText('note.contentOnPageLinkTo') + ': ' + #linkedPage.titles[currentLang.code]}" />
                        <s:set var="linkType" value="%{getText('note.contentLinkTo')}"/>
                    </s:if>
                    <div class="col-xs-12">
                        <label class="col-sm-1 text-right no-padding pr-10">
                            <span title="${linkType}"><s:text name="label.URL" /></span> </label>
                        <div class="col-sm-11 no-padding"><s:property value="linkDestination" /></div>
                    </div>
                    <div class="col-xs-12">
                        <label class="col-sm-1 no-padding text-right pr-10"><s:text name="label.text"/> </label>
                        <div class="col-sm-11 no-padding">
                            <wpsf:textfield id="%{#attributeTracer.getFormFieldName(#attribute)}"
                                            name="%{#attributeTracer.getFormFieldName(#attribute)}"
                                            value="%{#attribute.getTextForLang(#lang.code)}"
                                            maxlength="254" placeholder="%{getText('label.alt.text')}" title="%{getText('label.alt.text')}" cssClass="form-control" />
                        </div>
                    </div>
                </s:if>
            </div>
            <s:if test="#linkToEdit != null || ((#attributeTracer.monoListElement) || (#attributeTracer.compositeElement))">
                <div class="text-right">
            </s:if>
            <wpsf:submit cssClass="btn btn-default mr-10" type="button" action="%{#chooseLinkActionName}" title="%{#attribute.name + ': ' + getText('label.configure')}">
                <span class="sr-only"><s:property value="#attribute.name" />:</span>
                <s:text name="label.edit" />
            </wpsf:submit>
            <s:if test="%{(#lang.default && #linkToEdit != null && ((!#attributeTracer.monoListElement) || (#attributeTracer.monoListElement && #attributeTracer.compositeElement))) || 
                  (null != #currentLink && !#lang.default)}">
                <wpsa:actionParam action="removeLink" var="removeLinkActionName" >
                    <wpsa:actionSubParam name="parentAttributeName" value="%{#parentAttribute.name}" />
                    <wpsa:actionSubParam name="attributeName" value="%{#attribute.name}" />
                    <wpsa:actionSubParam name="elementIndex" value="%{#elementIndex}" />
                    <wpsa:actionSubParam name="langCodeOfLink" value="%{#lang.code}" />
                </wpsa:actionParam>
                <wpsf:submit cssClass="btn btn-danger" type="button"  action="%{#removeLinkActionName}" title="%{getText('label.remove')}">
                    <span class="sr-only"><s:text name="label.remove" /></span>
                    <s:text name="label.remove" />
                </wpsf:submit>
            </s:if>
            <s:if test="#linkToEdit != null || ((#attributeTracer.monoListElement) || (#attributeTracer.compositeElement))">
                </div>
            </s:if>
        </div>
    </div>
    <s:if test="#linkToEdit != null">
        </div>
    </s:if>
</s:if>
<s:else>
    <wpsf:submit cssClass="btn btn-primary" type="button" action="%{#chooseLinkActionName}" title="%{#attribute.name + ': ' + getText('label.configure')}">
        <span class="sr-only"><s:property value="#attribute.name" />:</span>
        <s:text name="label.add" />
    </wpsf:submit>
</s:else>
