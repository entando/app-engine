<#assign wpfp=JspTaglibs["/jpsolr-core"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpcms=JspTaglibs["/jacms-aps-core"]>

<@wp.headInfo type="CSS" info="../../plugins/jpsolr/jpsolr.css"/>
<div class="jpsolr">

<h2><@wp.i18n key="jpsolr_TITLE_FACET_RESULTS" /></h2>

<@wpfp.facetNavResult requiredFacetsParamName="requiredFacets"
	resultParamName="contentList" executeExtractRequiredFacets=false breadCrumbsParamName="breadCrumbs" />

<#if (breadCrumbs??) && (breadCrumbs?has_content)>
	<p><@wp.i18n key="SEARCHED_FOR" />:</p>
	<ul class="jpsolr_filterlist">
		<#list breadCrumbs as item>
		<li>
			<#list item.breadCrumbs as breadCrumb>
				<#if (breadCrumb_index != 0)>
					<#if (breadCrumb == item.requiredFacet)>
						<span class="jpfacetfilter jpsolr_requiredFacet"><@wpfp.facetNodeTitle facetNodeCode="${breadCrumb}" /></span>
                                                <#assign currentNodeTitle ><@wpfp.facetNodeTitle facetNodeCode="${breadCrumb}" /></#assign>
					<#elseif (breadCrumb == item.facetRoot)>
						<span class="jpfacetfilter jpsolr_facetRoot"><@wpfp.facetNodeTitle facetNodeCode="${item.facetRoot}" /></span>
                                                <#assign currentNodeTitle ><@wpfp.facetNodeTitle facetNodeCode="${item.facetRoot}" /></#assign>
					<#else>
						<a title="<@wp.i18n key="jpsolr_REMOVE_FILTER" />: <@wpfp.facetNodeTitle facetNodeCode="${breadCrumb}" />" class="jpfacetfilter" href="<@wpfp.url escapeAmp=false><@wpfp.urlPar name="selectedNode" >${breadCrumb}</@wpfp.urlPar>
						   <#list requiredFacets as requiredFacet>
						   <@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar>
						   </#list>
						   </@wpfp.url>"><@wpfp.facetNodeTitle facetNodeCode="${breadCrumb}" />
						</a>
                                                <#assign currentNodeTitle ><@wpfp.facetNodeTitle facetNodeCode="${breadCrumb}" /></#assign>
					</#if>
					<#if (item.breadCrumbs?size != (breadCrumb_index + 1))>&#32;/&#32;</#if>
				</#if>
			</#list>
			<span class="noscreen">|</span>&#32;<a class="jpsolrfilterremove" title="<@wp.i18n key="jpsolr_REMOVE_FILTER" />:&#32;${currentNodeTitle}" href="<@wpfp.url escapeAmp=false><#list requiredFacets as requiredFacet><@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar></#list><#list item.breadCrumbs as breadCrumb2><@wpfp.urlPar name="facetNodeToRemove_${breadCrumb2_index + 1}" >${breadCrumb2}</@wpfp.urlPar></#list></@wpfp.url>">
			<img src="<@wp.resourceURL />plugins/jpsolr/static/img/edit-delete.png" alt="<@wp.i18n key="jpsolr_REMOVE_FILTER" />" /></a>
		</li>
		</#list>
	</ul>
</#if>

<#if (contentList??) && (contentList?has_content)>
	<@wp.pager listName="contentList" objectName="groupContent" max=10 pagerIdFromFrame=true >
		<p><em><@wp.i18n key="SEARCH_RESULTS_INTRO" />&#32;${groupContent.size}&#32;<@wp.i18n key="SEARCH_RESULTS_OUTRO" />&#32;[${groupContent.begin + 1} &ndash; ${groupContent.end + 1}]:</em></p>
		<ol class="pureSize">
			<#list contentList as contentId>
			<#if (contentId_index >= groupContent.begin) && (contentId_index <= groupContent.end)>
			<li><@wpcms.content contentId="${contentId}" modelId="list" /></li>
			</#if>
			</#list>
		</ol>
		<#if (groupContent.size > groupContent.max)>
			<div>
				<p class="paginazione">
					<#if (1 == groupContent.currItem)>
					&laquo;&#32;<@wp.i18n key="PREV" />
					<#else>
					<a href="<@wp.url paramRepeat=true ><@wp.parameter name="${groupContent.paramItemName}" >${groupContent.prevItem}</@wp.parameter><#list requiredFacets as requiredFacet><@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar></#list></@wp.url>">&laquo;&#32;<@wp.i18n key="PREV" /></a>
					</#if>
					<#list groupContent.items as item>
						<#if (item == groupContent.currItem)>
						&#32;[${item}]&#32;
						<#else>
						&#32;<a href="<@wp.url paramRepeat=true ><@wp.parameter name="${groupContent.paramItemName}" >${item}</@wp.parameter><#list requiredFacets as requiredFacet><@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar></#list></@wp.url>">${item}</a>&#32;
						</#if>
					</#list>
					<#if (groupContent.maxItem == groupContent.currItem)>
					<@wp.i18n key="NEXT" />&#32;&raquo;
					<#else>
					<a href="<@wp.url paramRepeat=true ><@wp.parameter name="${groupContent.paramItemName}" >${groupContent.nextItem}</@wp.parameter><#list requiredFacets as requiredFacet><@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar></#list></@wp.url>"><@wp.i18n key="NEXT" />&#32;&raquo;</a>
					</#if>
				</p>
			</div>
		</#if>
	</@wp.pager>
<#else>
	<p><em><@wp.i18n key="SEARCH_NOTHING_FOUND" /></em></p>
</#if>
</div>