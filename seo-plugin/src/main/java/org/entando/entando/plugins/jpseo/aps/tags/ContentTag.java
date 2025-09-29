/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpseo.aps.tags;

import java.io.Serializable;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspException;

import org.entando.entando.plugins.jpseo.aps.system.JpseoSystemConstants;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.IContentViewerHelper;
import com.agiletec.plugins.jacms.aps.system.services.dispenser.ContentRenderizationInfo;

/**
 * @author E.Santoboni
 */
public class ContentTag extends com.agiletec.plugins.jacms.aps.tags.ContentTag {

	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(ContentTag.class);

	public ContentTag() {
		super();
	}
	
	@Override
	public int doStartTag() throws JspException {
		ServletRequest request =  this.pageContext.getRequest();
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
		try {
			IContentViewerHelper helper = (IContentViewerHelper) ApsWebApplicationUtils.getBean(JacmsSystemConstants.CONTENT_VIEWER_HELPER, this.pageContext);
			String contentId = (this.getContentId() != null && this.getContentId().trim().length() > 0) ? this.getContentId() : null;
			ContentRenderizationInfo renderInfo = helper.getRenderizationInfo(contentId, this.getModelId(), this.isPublishExtraTitle(), reqCtx);
			String renderedContent = (null != renderInfo) ? renderInfo.getRenderedContent() : "";
			if (null != this.getVar()) {
				this.pageContext.setAttribute(this.getVar(), renderedContent);
			} else {
				this.pageContext.getOut().print(renderedContent);
			}
			if (null != renderInfo) {
				if (null != this.getAttributeValuesByRoleVar()) {
					this.pageContext.setAttribute(this.getAttributeValuesByRoleVar(), renderInfo.getAttributeValues());
				}
				if (this.isPublishExtraDescription() && null != renderInfo.getAttributeValues()) {
					Serializable values = (Serializable) renderInfo.getAttributeValues().get(JpseoSystemConstants.ATTRIBUTE_ROLE_DESCRIPTION);
					if (null != values) {
						reqCtx.addExtraParam(JpseoSystemConstants.EXTRAPAR_EXTRA_PAGE_DESCRIPTIONS, values);
					}
				}
			}
		} catch (Throwable t) {
			_logger.error("error in doStartTag", t);
			throw new JspException("Error detected while initialising the tag", t);
		}
		return EVAL_PAGE;
	}
	
	@Override
	public void release() {
		super.release();
		this.setPublishExtraDescription(false);
	}
	
	/**
	 * Return true if the extra descriptions will be insert into Request Context.
	 * @return The property.
	 */
	public boolean isPublishExtraDescription() {
		return _publishExtraDescription;
	}
	
	/**
	 * Specify if the extra descriptions will be insert into Request Context.
	 * @param publishExtraDescription The property to set.
	 */
	public void setPublishExtraDescription(boolean publishExtraDescription) {
		this._publishExtraDescription = publishExtraDescription;
	}
	
	private boolean _publishExtraDescription;
	
}