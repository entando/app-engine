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

import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;

import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.ent.exception.EntException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.entando.entando.plugins.jpsolr.aps.system.content.widget.IFacetNavHelper;

/**
 * @author E.Santoboni
 */
public class FacetNavTreeTag extends AbstractFacetNavTag {

	private static final Logger logger = LoggerFactory.getLogger(FacetNavTreeTag.class);
	
	@Override
	public int doStartTag() throws JspException {
		ServletRequest request =  this.pageContext.getRequest();
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
		try {
			List<String> requiredFacets = this.getRequiredFacets();
            IFacetNavHelper facetNavHelper = (IFacetNavHelper) ApsWebApplicationUtils.getBean(JpSolrSystemConstants.CONTENT_FACET_NAV_HELPER, this.pageContext);
			FacetedContentsResult result = facetNavHelper.getResult(requiredFacets, reqCtx);
			reqCtx.addExtraParam(SOLR_RESULT_REQUEST_PARAM, result);
            Map<String, Integer> occurrences = result.getOccurrences();
			List<ITreeNode> facetsForTree = this.getFacetRootNodes(reqCtx);
			this.pageContext.setAttribute(this.getFacetsTreeParamName(), facetsForTree);
			request.setAttribute(this.getOccurrencesParamName(), occurrences);
			request.setAttribute(this.getRequiredFacetsParamName(), requiredFacets);
		} catch (EntException | RuntimeException ex) {
			throw new JspException("Error initialization tag", ex);
		}
		return super.doStartTag();
	}

	protected List<ITreeNode> getFacetRootNodes(RequestContext reqCtx) {
		List<ITreeNode> facets = null;
		Widget currentWidget = (Widget) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET);
		String configParamName = JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME;
		String facetParamConfig = currentWidget.getConfig().getProperty(configParamName);
		if (null != facetParamConfig && facetParamConfig.trim().length()>0) {
			facets = super.getFacetRoots(facetParamConfig);
		}
		return facets;
	}

	public String getFacetsTreeParamName() {
		return facetsTreeParamName;
	}
	public void setFacetsTreeParamName(String facetsTreeParamName) {
		this.facetsTreeParamName = facetsTreeParamName;
	}
	
	private String facetsTreeParamName;

}
