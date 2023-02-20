<#assign wpfp=JspTaglibs["/jpsolr-core"]>
<#assign wp=JspTaglibs["/aps-core"]>
<@wp.headInfo type="CSS" info="../../plugins/jpsolr/static/css/jpsolr.css"/>
<div class="jpsolr">
	<h2 class="title"><@wp.i18n key="jpsolr_TITLE_TREE" /></h2>
	<@wpfp.facetNavTree requiredFacetsParamName="requiredFacets" facetsTreeParamName="facetsForTree" />
	<@wp.freemarkerTemplateParameter var="occurrences" valueName="occurrences" removeOnEndTag=true >
	<#list facetsForTree as facetRoot>
		<h3><@wpfp.facetNodeTitle facetNodeCode="${facetRoot.code}" /></h3>
		<#if (occurrences[facetRoot.code]??) && (occurrences[facetRoot.code]?has_content)>
			<ul>
				<#list facetRoot.childrenCodes as facetNodeCode>
					<#if (occurrences[facetNodeCode]??)>
						<li>
							<a href="<@wpfp.url escapeAmp=false><@wp.parameter name="selectedNode" >${facetNodeCode}</@wp.parameter>
								<#list requiredFacets as requiredFacet>
								<@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar>
								</#list>
								</@wpfp.url>"><@wpfp.facetNodeTitle facetNodeCode="${facetNodeCode}" /></a>&#32;<abbr class="jpsolr_tree_occurences" title="<@wp.i18n key="jpsolr_OCCURRENCES_FOR" />&#32;<@wpfp.facetNodeTitle facetNodeCode="${facetNodeCode}" />:&#32;${occurrences[facetNodeCode]}">${occurrences[facetNodeCode]}</abbr>
                                                        <@wpfp.hasToOpenFacetNode facetNodeCode="${facetNodeCode}" requiredFacetsParamName="requiredFacets" occurrencesParamName="occurrences" >
                                                                <@wp.freemarkerTemplateParameter var="currFacetRootCode" valueName="facetNodeCode" removeOnEndTag=true >
								<@wp.fragment code="jpsolr_inc_facetNavTree" escapeXml=false />
								</@wp.freemarkerTemplateParameter>
							</@wpfp.hasToOpenFacetNode>
						</li>
					</#if>
				</#list>
			</ul>
		<#else>
			<p><abbr title="<@wp.i18n key="jpsolr_EMPTY_TAG" />:&#32;<@wpfp.facetNodeTitle facetNodeCode="${facetRoot.code}" escapeXml=true />">&ndash;</abbr></p>
		</#if>
	</#list>
	</@wp.freemarkerTemplateParameter>
</div>