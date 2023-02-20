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
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.content.widget.IFacetNavHelper;

/**
 * @author E.Santoboni
 */
public class FacetNavResultTag extends AbstractFacetNavTag {

    @Override
    public int doStartTag() throws JspException {
        ServletRequest request = this.pageContext.getRequest();
        RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
        try {
            List<String> requiredFacets;
            if (this.isExecuteExtractRequiredFacets()) {
                requiredFacets = this.getRequiredFacets();
                this.pageContext.setAttribute(this.getRequiredFacetsParamName(), requiredFacets);
            } else {
                requiredFacets = (List<String>) request.getAttribute(this.getRequiredFacetsParamName());
                if (requiredFacets == null) {
                    requiredFacets = new ArrayList<>();
                }
            }
            IFacetNavHelper facetNavHelper = ApsWebApplicationUtils.getBean(IFacetNavHelper.class, this.pageContext);
            FacetedContentsResult result = (FacetedContentsResult) reqCtx.getExtraParam(SOLR_RESULT_REQUEST_PARAM);
            if (null == result) {
                result = facetNavHelper.getResult(requiredFacets, reqCtx);
            }
            this.pageContext.setAttribute(this.getResultParamName(), result.getContentsId());
            if (null != this.getBreadCrumbsParamName()) {
                this.pageContext.setAttribute(this.getBreadCrumbsParamName(),
                        super.getBreadCrumbs(requiredFacets, reqCtx));
            }
        } catch (EntException | RuntimeException ex) {
            throw new JspException("error in startTag", ex);
        }
        return super.doStartTag();
    }

    public String getResultParamName() {
        return resultParamName;
    }

    public void setResultParamName(String resultParamName) {
        this.resultParamName = resultParamName;
    }

    public boolean isExecuteExtractRequiredFacets() {
        return executeExtractRequiredFacets;
    }

    public void setExecuteExtractRequiredFacets(boolean executeExtractRequiredFacets) {
        this.executeExtractRequiredFacets = executeExtractRequiredFacets;
    }

    public String getBreadCrumbsParamName() {
        return breadCrumbsParamName;
    }

    public void setBreadCrumbsParamName(String breadCrumbsParamName) {
        this.breadCrumbsParamName = breadCrumbsParamName;
    }

    private String resultParamName;
    private boolean executeExtractRequiredFacets = true;
    private String breadCrumbsParamName;

}
