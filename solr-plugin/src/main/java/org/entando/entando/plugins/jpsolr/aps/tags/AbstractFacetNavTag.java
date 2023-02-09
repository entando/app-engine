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
import java.util.Enumeration;
import java.util.Iterator;
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
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (paramName.equals(nodesParamName)) {
                    String[] values = request.getParameterValues(paramName);
                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];
                        if (!StringUtils.isBlank(value)) {
                            this.addFacet(requiredFacets, StringEscapeUtils.unescapeHtml4(value));
                        }
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
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
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
        Iterator<String> requredFacetIterator = requiredFacets.iterator();
        ITreeNodeManager facetManager = this.getFacetManager();
        while (requredFacetIterator.hasNext()) {
            String reqNode = requredFacetIterator.next();
            ITreeNode currentNode = facetManager.getNode(reqNode);
            ITreeNode parent = facetManager.getNode(currentNode.getParentCode());
			if (this.isChildOf(parent, selectedNode)) {
				nodesToRemove.add(reqNode);
			}
        }
        for (int i = 0; i < nodesToRemove.size(); i++) {
            String nodeToRemove = nodesToRemove.get(i);
            requiredFacets.remove(nodeToRemove);
        }
    }

    /**
     * Returns facet manager
     *
     * @return Facet manager
     */
    protected ITreeNodeManager getFacetManager() {
        IFacetNavHelper facetNavHelper = (IFacetNavHelper) ApsWebApplicationUtils.getBean(
                JpSolrSystemConstants.CONTENT_FACET_NAV_HELPER, this.pageContext);
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
        for (int i = 0; i < finalNodes.size(); i++) {
            ITreeNode requiredNode = finalNodes.get(i);
            for (int j = 0; j < roots.size(); j++) {
                ITreeNode root = roots.get(j);
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
        for (int i = 0; i < requiredFacets.size(); i++) {
            String nodeToAnalize = requiredFacets.get(i);
            this.removeParentOf(nodeToAnalize, requiredFacetsCopy);
        }
        Iterator<String> requiredFacetIterator = requiredFacetsCopy.iterator();
        while (requiredFacetIterator.hasNext()) {
            String reqNode = requiredFacetIterator.next();
            finalNodes.add(this.getFacetManager().getNode(reqNode));
        }
        return finalNodes;
    }

    /**
     * Remove node parent
     *
     * @param nodeFromAnalize
     * @param requiredFacetsCopy
     */
    private void removeParentOf(String nodeFromAnalize, List<String> requiredFacetsCopy) {
        ITreeNode nodeFrom = this.getFacetManager().getNode(nodeFromAnalize);
        List<String> nodesToRemove = new ArrayList<>();
        Iterator<String> requiredFacetIterator = requiredFacetsCopy.iterator();
        while (requiredFacetIterator.hasNext()) {
            String reqNode = requiredFacetIterator.next();
            if (!nodeFromAnalize.equals(reqNode) && this.isChildOf(nodeFrom, reqNode)) {
                nodesToRemove.add(reqNode);
            }
        }
        for (int i = 0; i < nodesToRemove.size(); i++) {
            String nodeToRemove = nodesToRemove.get(i);
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
        for (int j = 0; j < facetCodes.length; j++) {
            String facetCode = facetCodes[j].trim();
            ITreeNode node = this.getFacetManager().getNode(facetCode);
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