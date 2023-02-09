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

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;

/**
 * @author E.Santoboni
 */
public class HasToOpenFacetNodeTag extends AbstractFacetNavTag {

	@Override
	public int doStartTag() throws JspException {
		try {
			boolean hasToOpen
					= ((this.getRequiredFacets().contains(this.getFacetNodeCode())) || this.isSelectedOneChild())
					&& this.hasChildrenOccurrences();
			if (hasToOpen) {
				return EVAL_BODY_INCLUDE;
			} else {
				return super.doStartTag();
			}
		} catch (RuntimeException ex) {
			throw new JspException("Error doStartTag", ex);
		}
	}

	/**
	 * Returns true if one child is selected
	 *
	 * @return True if one child is selected
	 */
	private boolean isSelectedOneChild() {
		ITreeNodeManager facetManager = this.getFacetManager();
		List<String> facets = this.getRequiredFacets();
		for (int i = 0; i < facets.size(); i++) {
			String requiredFacet = facets.get(i);
			ITreeNode facet = facetManager.getNode(requiredFacet);
			if (null != facet) {
				boolean check = this.checkSelectChild(facet, this.getFacetNodeCode(), facetManager);
				if (check) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if a child is selected.
	 *
	 * @param facet
	 * @param codeForCheck
	 * @return true if a child is selected
	 */
	private boolean checkSelectChild(ITreeNode facet, String codeForCheck, ITreeNodeManager facetManager) {
		if (facet.getCode().equals(codeForCheck)) {
			return true;
		}
		ITreeNode parentFacet = facetManager.getNode(facet.getParentCode());
		if (null != parentFacet && !parentFacet.getCode().equals(parentFacet.getParentCode())) {
			return this.checkSelectChild(parentFacet, codeForCheck, facetManager);
		}
		return false;
	}

	/**
	 * Returns true if there are children occurrences.
	 *
	 * @return true if there are children occurrences
	 */
	private boolean hasChildrenOccurrences() {
		ITreeNodeManager facetManager = this.getFacetManager();
		ITreeNode facet = facetManager.getNode(this.getFacetNodeCode());
		for (int i = 0; i < facet.getChildrenCodes().length; i++) {
			String code = facet.getChildrenCodes()[i];
			ITreeNode child = facetManager.getNode(code);
			Integer occurrence = this.getOccurrences().get(child.getCode());
			if (null != occurrence && occurrence.intValue() > 0) {
				return true;
			}
		}
		return false;
	}

	public String getFacetNodeCode() {
		return facetNodeCode;
	}

	public void setFacetNodeCode(String facetNodeCode) {
		this.facetNodeCode = facetNodeCode;
	}

	@Override
	public List<String> getRequiredFacets() {
		if (null == this.requiredFacets) {
			if (null == this.getRequiredFacetsParamName()) {
				return new ArrayList<>();
			} else {
				ServletRequest request = this.pageContext.getRequest();
				List<String> list = (List<String>) request.getAttribute(this.getRequiredFacetsParamName());
				if (null == list) {
					return new ArrayList<>();
				} else {
					return list;
				}
			}
		}
		return requiredFacets;
	}

	public void setRequiredFacets(List<String> requiredFacets) {
		this.requiredFacets = requiredFacets;
	}

	public Map<String, Integer> getOccurrences() {
		if (null == this.occurrences) {
			if (null == this.getOccurrencesParamName()) {
				return new HashMap<>();
			} else {
				ServletRequest request = this.pageContext.getRequest();
				Map<String, Integer> map = (Map<String, Integer>) request.getAttribute(this.getOccurrencesParamName());
				if (null == map) {
					return new HashMap<>();
				} else {
					return map;
				}
			}
		}
		return occurrences;
	}

	public void setOccurrences(Map<String, Integer> occurrences) {
		this.occurrences = occurrences;
	}

	private String facetNodeCode;//="${facetNode.code}"
	private List<String> requiredFacets;//="requiredFacets"
	private Map<String, Integer> occurrences; //="${occurrences}"

}
