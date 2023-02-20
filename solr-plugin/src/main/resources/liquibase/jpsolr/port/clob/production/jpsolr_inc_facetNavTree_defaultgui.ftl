<#assign wpfp=JspTaglibs["/jpsolr-core"]>
<#assign wp=JspTaglibs["/aps-core"]>
<ul>
    <@wp.categories var="childrensVar" root="${currFacetRootCode}" />
    <#if childrensVar??>
        <#list childrensVar as facetNode>
            <@wpfp.hasToViewFacetNode facetNodeCode="${facetNode.key}" requiredFacetsParamName="requiredFacets">
                <#if (occurrences[facetNode.key]??)>
                    <li>
                    <a href="<@wpfp.url escapeAmp=false><@wpfp.urlPar name="selectedNode" >${facetNode.key}</@wpfp.urlPar>
                        <#list requiredFacets as requiredFacet>
                        <@wpfp.urlPar name="facetNode_${requiredFacet_index + 1}" >${requiredFacet}</@wpfp.urlPar>
                        </#list>
                        </@wpfp.url>"><@wpfp.facetNodeTitle escapeXml=true facetNodeCode="${facetNode.key}" /></a>&#32;<abbr class="jpsolr_tree_occurences" title="<@wp.i18n key="jpsolr_OCCURRENCES_FOR" />&#32;<@wpfp.facetNodeTitle escapeXml=true facetNodeCode="${facetNode.key}" />:&#32;${occurrences[facetNode.key]}">${occurrences[facetNode.key]}</abbr>
                    <@wpfp.hasToOpenFacetNode facetNodeCode="${facetNode.key}" requiredFacetsParamName="requiredFacets" occurrencesParamName="occurrences" >
                        <@wp.freemarkerTemplateParameter var="currFacetRootCode" valueName="${facetNode.key}" removeOnEndTag=true >
                        <@wp.fragment code="jpsolr_inc_facetNavTree" escapeXml=false />
                        </@wp.freemarkerTemplateParameter>
                    </@wpfp.hasToOpenFacetNode>
                    </li>
                </#if>
            </@wpfp.hasToViewFacetNode>
        </#list>
    </#if>
</ul>