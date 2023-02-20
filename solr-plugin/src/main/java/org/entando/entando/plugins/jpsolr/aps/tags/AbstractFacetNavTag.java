/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpsolr.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.entando.entando.plugins.jpsolr.aps.system.content.widget.IFacetNavHelper;
import org.entando.entando.plugins.jpsolr.aps.tags.util.FacetBreadCrumbs;

/**
 * @author E.Santoboni
 */
public abstract class AbstractFacetNavTag extends TagSupport {

    protected static final String SOLR_RESULT_REQUEST_PARAM = "jpsolr_searchResult";

    /**
     * Returns required facets
     *
     * @return Required facets
     */
    protected List<String> getRequiredFacets() {
        List<String> requiredFacets = new ArrayList<>();
        try {
            ServletRequest request = this.pageContext.getRequest();
            String nodesParamName = this.getFacetNodesParamName();
            if (null == nodesParamName) {
                nodesParamName = "facetNode";
            }
            int index = 1;
            while (null != request.getParameter(nodesParamName + "_" + index)) {
                String paramName = nodesParamName + "_" + index;
                String value = StringEscapeUtils.unescapeHtml4(request.getParameter(paramName));
                this.addFacet(requiredFacets, value);
                index++;
            }
            String[] values = request.getParameterValues("nodesParamName");
            if (values != null) {
                for (String value : values) {
                    if (!StringUtils.isBlank(value)) {
                        this.addFacet(requiredFacets, StringEscapeUtils.unescapeHtml4(value));
                    }
                }
            }
            String selectedNode = request.getParameter("selectedNode");
            if (!StringUtils.isBlank(selectedNode)) {
                selectedNode = StringEscapeUtils.unescapeHtml4(selectedNode);
                this.addFacet(requiredFacets, selectedNode);
            }
            this.removeSelections(requiredFacets);
            this.manageCurrentSelect(selectedNode, requiredFacets);
        } catch (Exception ex) {
            throw new EntRuntimeException("Error extracting required facets", ex);
        }
        return requiredFacets;
    }

    /**
     * Delete the facets selected through checkboxes selected.
     *
     * @param requiredFacets
     */
    private void removeSelections(List<String> requiredFacets) {
        String nodeToRemoveParamName = "facetNodeToRemove";
        ServletRequest request = this.pageContext.getRequest();
        String[] values = request.getParameterValues(nodeToRemoveParamName);
        if (null != values) {
            for (String value : values) {
                requiredFacets.remove(value);
            }
        }
        int index = 1;
        while (null != request.getParameter(nodeToRemoveParamName + "_" + index)) {
            String paramName = nodeToRemoveParamName + "_" + index;
            String value = request.getParameter(paramName);
            requiredFacets.remove(value);
            index++;
        }
    }

    /**
     * MANAGEMENT OF SELECTED current node (child nodes REMOVE ANY OF THE SELECTION).
     *
     * @param selectedNode
     * @param requiredFacets
     */
    private void manageCurrentSelect(String selectedNode, List<String> requiredFacets) {
        List<String> nodesToRemove = new ArrayList<>();
        ITreeNodeManager facetManager = this.getFacetManager();
        for (String reqNode : requiredFacets) {
            ITreeNode currentNode = facetManager.getNode(reqNode);
            ITreeNode parent = facetManager.getNode(currentNode.getParentCode());
            if (this.isChildOf(parent, selectedNode)) {
                nodesToRemove.add(reqNode);
            }
        }
        for (String nodeToRemove : nodesToRemove) {
            requiredFacets.remove(nodeToRemove);
        }
    }

    /**
     * Returns facet manager
     *
     * @return Facet manager
     */
    protected ITreeNodeManager getFacetManager() {
        IFacetNavHelper facetNavHelper = ApsWebApplicationUtils.getBean(IFacetNavHelper.class, this.pageContext);
        return facetNavHelper.getTreeNodeManager();
    }

    /**
     * Return true if it is a child of checked node
     *
     * @param nodeToCheck
     * @param codeForCheck
     * @return True if it is a child of checked node
     */
    protected boolean isChildOf(ITreeNode nodeToCheck, String codeForCheck) {
        if (nodeToCheck.getCode().equals(codeForCheck)) {
            return true;
        }
        ITreeNode parentFacet = this.getFacetManager().getNode(nodeToCheck.getParentCode());
        if (null != parentFacet && !parentFacet.getCode().equals(parentFacet.getParentCode())) {
            return this.isChildOf(parentFacet, codeForCheck);
        }
        return false;
    }

    /**
     * Add new facet
     *
     * @param requiredFacets
     * @param value
     */
    private void addFacet(List<String> requiredFacets, String value) {
        if (null != value && value.trim().length() > 0 && !requiredFacets.contains(value.trim())) {
            requiredFacets.add(value.trim());
        }
    }

    /**
     * Returns the list of objects (@ link FacetBreadCrumbs).
     *
     * @param requiredFacets Nodes facets required.
     * @param reqCtx         The context of the current request.
     * @return The list of objects Breadcrumbs.
     */
    protected List<FacetBreadCrumbs> getBreadCrumbs(List<String> requiredFacets, RequestContext reqCtx) {
        List<ITreeNode> roots = this.getFacetRoots(reqCtx);
        if (roots.isEmpty()) {
            return new ArrayList<>();
        }
        List<ITreeNode> finalNodes = this.getFinalNodes(requiredFacets);
        List<FacetBreadCrumbs> breadCrumbs = new ArrayList<>();
        for (ITreeNode requiredNode : finalNodes) {
            for (ITreeNode root : roots) {
                if (this.isChildOf(requiredNode, root.getCode())) {
                    breadCrumbs.add(
                            new FacetBreadCrumbs(requiredNode.getCode(), root.getCode(), this.getFacetManager()));
                }
            }
        }
        return breadCrumbs;
    }

    /**
     * Returns final nodes
     *
     * @param requiredFacets
     * @return Final Nodes
     */
    private List<ITreeNode> getFinalNodes(List<String> requiredFacets) {
        List<ITreeNode> finalNodes = new ArrayList<>();
        List<String> requiredFacetsCopy = new ArrayList<>(requiredFacets);
        for (String nodeToAnalyze : requiredFacets) {
            this.removeParentOf(nodeToAnalyze, requiredFacetsCopy);
        }
        for (String reqNode : requiredFacetsCopy) {
            finalNodes.add(this.getFacetManager().getNode(reqNode));
        }
        return finalNodes;
    }

    /**
     * Remove node parent
     *
     * @param nodeFromAnalyze
     * @param requiredFacetsCopy
     */
    private void removeParentOf(String nodeFromAnalyze, List<String> requiredFacetsCopy) {
        ITreeNode nodeFrom = this.getFacetManager().getNode(nodeFromAnalyze);
        List<String> nodesToRemove = new ArrayList<>();
        for (String reqNode : requiredFacetsCopy) {
            if (!nodeFromAnalyze.equals(reqNode) && this.isChildOf(nodeFrom, reqNode)) {
                nodesToRemove.add(reqNode);
            }
        }
        for (String nodeToRemove : nodesToRemove) {
            requiredFacetsCopy.remove(nodeToRemove);
        }
    }

    /**
     * Returns a list of root nodes through which grant the tree. The root nodes allow you to create blocks of selected
     * nodes in showlet appropriate.
     *
     * @param reqCtx The context of the current request.
     * @return The list of root nodes.
     */
    protected List<ITreeNode> getFacetRoots(RequestContext reqCtx) {
        IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        Integer currentFrame = (Integer) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME);
        Widget[] widgets = page.getWidgets();
        for (int i = 0; i < widgets.length; i++) {
            if (i == currentFrame.intValue()) {
                continue;
            }
            Widget widget = widgets[i];
            String configParamName = JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME;
            if (null != widget && null != widget.getConfig()
                    && null != widget.getConfig().getProperty(configParamName)) {
                String facetParamConfig = widget.getConfig().getProperty(configParamName);
                return this.getFacetRoots(facetParamConfig);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Returns facet roots.
     *
     * @param facetRootNodesParam
     * @return facet roots
     */
    protected List<ITreeNode> getFacetRoots(String facetRootNodesParam) {
        List<ITreeNode> nodes = new ArrayList<>();
        String[] facetCodes = facetRootNodesParam.split(",");
        for (String facetCode : facetCodes) {
            ITreeNode node = this.getFacetManager().getNode(facetCode.trim());
            if (null != node) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public String getFacetNodesParamName() {
        return facetNodesParamName;
    }

    public void setFacetNodesParamName(String facetNodesParamName) {
        this.facetNodesParamName = facetNodesParamName;
    }

    public String getRequiredFacetsParamName() {
        return requiredFacetsParamName;
    }

    public void setRequiredFacetsParamName(String requiredFacetsParamName) {
        this.requiredFacetsParamName = requiredFacetsParamName;
    }

    public String getOccurrencesParamName() {
        if (null == this.occurrencesParamName) {
            return "occurrences";
        }
        return occurrencesParamName;
    }

    public void setOccurrencesParamName(String occurrencesParamName) {
        this.occurrencesParamName = occurrencesParamName;
    }

    private String facetNodesParamName;
    private String requiredFacetsParamName;
    private String occurrencesParamName;

}