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
package org.entando.entando.plugins.jpsolr.apsadmin.portal.specialwidget;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.apsadmin.system.ITreeAction;
import com.agiletec.apsadmin.system.ITreeNodeBaseActionHelper;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.entando.entando.plugins.jpsolr.apsadmin.portal.specialwidget.util.FacetNavWidgetHelper;

/**
 * @author E.Santoboni
 */
public class FacetNavTreeWidgetAction extends FacetNavResultWidgetAction implements ITreeAction {
	
	@Override
	public void validate() {
		super.validate();
		try {
			this.validateFacets();
		} catch (Throwable t) {
			ApsSystemUtils.logThrowable(t, this, "validate");
		}
	}
	
	protected void validateFacets() {
		List<String> facetCodes = this.getFacetRootCodes();
		for (String facetCode : facetCodes) {
			if (null == this.getFacet(facetCode)) {
				String[] args = { facetCode };
				String fieldName = JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME;
				this.addFieldError(fieldName, this.getText("message.facetNavWidget.facets.notValid", args));
			}
		}
	}
	
	/**
	 * Prepare action with the parameters contained in showlet.
	 */
	@Override
	protected void initSpecialParams() {
		super.initSpecialParams();
		if (null != this.getWidget().getConfig()) {
			String configParamName = JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME;
			String facetsParam = this.getWidget().getConfig().getProperty(configParamName);
			this.setFacetRootNodes(facetsParam);
		}
	}
	
	/**
	 * Add a facet to the associated facet nodes
	 * @return The code describing the result of the operation.
	 */
	public String joinFacet() {
		try {
			this.createValuedShowlet();
			if (this.isValidFacet()) {
				String facetCode = this.getFacetCode();
				List<String> facetCodes = this.getFacetRootCodes();
				ITreeNode facet = this.getTreeNodeManager().getNode(this.getFacetCode());
				if (facet != null && !facet.getCode().equals(facet.getParentCode()) && !facetCodes.contains(facetCode)) {//se esiste, non è la Home e non è 
					facetCodes.add(facetCode);
					String facetsFilter = FacetNavWidgetHelper.concatStrings(facetCodes, ",");
					String configParamName = JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME;
					this.getWidget().getConfig().setProperty(configParamName, facetsFilter);
					this.setFacetRootNodes(facetsFilter);
				}
			}
		} catch (Throwable t) {
			ApsSystemUtils.logThrowable(t, this, "joinFacet");
			return FAILURE;
		}
		return SUCCESS;
	}
	
	/**
	 * Remove a facet from the associated facet nodes
	 * @return The code describing the result of the operation.
	 */
	public String removeFacet() {
		try {
			this.createValuedShowlet();
			String facetCode = this.getFacetCode();
			List<String> facetCodes = this.getFacetRootCodes();
			if (facetCode != null) {
                facetCodes.remove(facetCode);
				String facetsFilter = FacetNavWidgetHelper.concatStrings(facetCodes, ",");
				String configParamName = JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME;
				this.getWidget().getConfig().setProperty(configParamName, facetsFilter);
				this.setFacetRootNodes(facetsFilter);
			}
		} catch (Throwable t) {
			ApsSystemUtils.logThrowable(t, this, "removeFacet");
			return FAILURE;
		}
		return SUCCESS;
	}

	/**
	 * Returns true if the facet is valid
	 * @return true if the facet is valid
	 */
	private boolean isValidFacet() {
		String facetCode = this.getFacetCode();
		return (facetCode != null && this.getFacet(facetCode) != null);
	}

	public ITreeNode getFacetRoot() {
		return this.getTreeNodeManager().getRoot();
	}

	public ITreeNode getFacet(String facetCode) {
		return this.getTreeNodeManager().getNode(facetCode);
	}

	public String getFacetCode() {
		return facetCode;
	}
	public void setFacetCode(String facetCode) {
		this.facetCode = facetCode;
	}

	public List<String> getFacetRootCodes() {
		String facetsParam = this.getFacetRootNodes();
		return FacetNavWidgetHelper.splitValues(facetsParam, ",");
	}

	public String getFacetRootNodes() {
		return facetRootNodes;
	}
	public void setFacetRootNodes(String facetRootNodes) {
		this.facetRootNodes = facetRootNodes;
	}
	
	@Override
	public String buildTree() {
		Set<String> targets = this.getTreeNodesToOpen();
		try {
			this.createValuedShowlet();
			String marker = this.getTreeNodeActionMarkerCode();
			if (null != marker) {
				if (marker.equalsIgnoreCase(ACTION_MARKER_OPEN)) {
					targets = this.getTreeHelper().checkTargetNodes(this.getTargetNode(), targets, this.getNodeGroupCodes());
				} else if (marker.equalsIgnoreCase(ACTION_MARKER_CLOSE)) {
					targets = this.getTreeHelper().checkTargetNodesOnClosing(this.getTargetNode(), targets, this.getNodeGroupCodes());
				}
			}
			this.setTreeNodesToOpen(targets);
		} catch (Throwable t) {
			ApsSystemUtils.logThrowable(t, this, "buildTree");
			return FAILURE;
		}
		return SUCCESS;
	}
	
	@Override
	public ITreeNode getShowableTree() {
		ITreeNode node = null;
		try {
			ITreeNode allowedTree = this.getAllowedTreeRootNode();
			node = this.getTreeHelper().getShowableTree(this.getTreeNodesToOpen(), allowedTree, this.getNodeGroupCodes());
		} catch (Throwable t) {
			ApsSystemUtils.logThrowable(t, this, "getShowableTree");
		}
		return node;
	}
	
	@Override
	public ITreeNode getAllowedTreeRootNode() {
		ITreeNode node = null;
		try {
			node = this.getTreeHelper().getAllowedTreeRoot(this.getNodeGroupCodes());
		} catch (Throwable t) {
			ApsSystemUtils.logThrowable(t, this, "getAllowedTreeRootNode");
		}
		return node;
	}
	
	/**
	 * Return the allowed codes of the group of the nodes to manage.
	 * This method has to be extended if the helper manage tree nodes with authority.
	 * @return The allowed group codes.
	 */
	protected Collection<String> getNodeGroupCodes() {
		return null;
	}
	
	public String getTargetNode() {
		return targetNode;
	}
	public void setTargetNode(String targetNode) {
		this.targetNode = targetNode;
	}
	
	public Set<String> getTreeNodesToOpen() {
		return treeNodesToOpen;
	}
	public void setTreeNodesToOpen(Set<String> treeNodesToOpen) {
		this.treeNodesToOpen = treeNodesToOpen;
	}
	
	public String getTreeNodeActionMarkerCode() {
		return treeNodeActionMarkerCode;
	}
	public void setTreeNodeActionMarkerCode(String treeNodeActionMarkerCode) {
		this.treeNodeActionMarkerCode = treeNodeActionMarkerCode;
	}
	
	protected ITreeNodeManager getTreeNodeManager() {
		return treeNodeManager;
	}
	public void setTreeNodeManager(ITreeNodeManager treeNodeManager) {
		this.treeNodeManager = treeNodeManager;
	}
	
	protected ITreeNodeBaseActionHelper getTreeHelper() {
		return treeHelper;
	}
	public void setTreeHelper(ITreeNodeBaseActionHelper treeHelper) {
		this.treeHelper = treeHelper;
	}
	
	private String facetCode;
	private String facetRootNodes;
	
	private String targetNode;
	private Set<String> treeNodesToOpen = new HashSet<>();
	
	private String treeNodeActionMarkerCode;

	private transient ITreeNodeManager treeNodeManager;
	
	private transient ITreeNodeBaseActionHelper treeHelper;

}