/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormListTag extends TagSupport {

	private static final Logger _logger =  LoggerFactory.getLogger(FormListTag.class);

	@Override
	public int doStartTag() throws JspException {
		ServletRequest request =  this.pageContext.getRequest();
		IFormManager formManager = (IFormManager) ApsWebApplicationUtils.getBean("jpwebformFormManager", this.pageContext);
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);

		Widget widget = extractWidget(reqCtx);
		ApsProperties prop = widget.getConfig();

		try {

			List<Long> list = formManager.getForms();

			String id = extractWidgetParameter("id",prop,reqCtx);
			String name = extractWidgetParameter("name",prop,reqCtx);
			String submitted = extractWidgetParameter("submitted", prop, reqCtx);
			String from = extractWidgetParameter("submitted",prop,reqCtx);
			String to = extractWidgetParameter("submitted", prop, reqCtx);
			String delivered = extractWidgetParameter("delivered", prop, reqCtx);
			String campagna = extractWidgetParameter("campagna", prop, reqCtx);

			if(StringUtils.isNotBlank(id)){
				List<FieldSearchFilter> filterList = new ArrayList<>();
				FieldSearchFilter idFilter = new FieldSearchFilter("id", id, delivered);
				filterList.add(idFilter);
				this.pageContext.setAttribute(this.getVar(), filterList);
			}

			if(StringUtils.isNotBlank(name)){
				List<FieldSearchFilter> filterList = new ArrayList<>();
				FieldSearchFilter idFilter = new FieldSearchFilter("name", name, delivered);
				filterList.add(idFilter);
				this.pageContext.setAttribute(this.getVar(), filterList);
			}

			if(StringUtils.isNotBlank(submitted)){
				List<FieldSearchFilter> filterList = new ArrayList<>();
				FieldSearchFilter idFilter = new FieldSearchFilter("submitted", from, to);
				filterList.add(idFilter);
				this.pageContext.setAttribute(this.getVar(), filterList);
			}

			if(StringUtils.isNotBlank(delivered)){
				List<FieldSearchFilter> filterList = new ArrayList<>();
				FieldSearchFilter idFilter = new FieldSearchFilter("delvered", delivered, delivered);
				filterList.add(idFilter);
				this.pageContext.setAttribute(this.getVar(), filterList);
			}


			FieldSearchFilter idFilter = new FieldSearchFilter("id", id, delivered);
			this.pageContext.setAttribute(this.getVar(), list);
		} catch (Throwable t) {
			_logger.error("Error in doStartTag", t);
			throw new JspException("Error in FormListTag doStartTag", t);
		}
		return super.doStartTag();
	}



	private Widget extractWidget(RequestContext reqCtx) {
		Widget widget = null;
		widget = (Widget) reqCtx.getExtraParam((SystemConstants.EXTRAPAR_CURRENT_WIDGET));
		return widget;
	}

	protected String extractWidgetParameter(String parameterName, ApsProperties widgetConfig, RequestContext reqCtx) {
		return (String) widgetConfig.get(parameterName);
	}

	@Override
	public int doEndTag() throws JspException {
		this.release();
		return super.doEndTag();
	}

	@Override
	public void release() {
		super.release();
		this.setVar(null);
	}

	public String getVar() {
		return _var;
	}
	public void setVar(String var) {
		this._var = var;
	}

	private String _var;

}
