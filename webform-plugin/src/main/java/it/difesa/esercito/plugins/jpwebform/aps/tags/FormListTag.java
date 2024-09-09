/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormListTag extends TagSupport {

	private static final Logger _logger =  LoggerFactory.getLogger(FormListTag.class);

	@Override
	public int doStartTag() throws JspException {
		ServletRequest request =  this.pageContext.getRequest();
		IFormManager formManager = (IFormManager) ApsWebApplicationUtils.getBean("jpwebformFormManager", this.pageContext);
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
		try {
			/*
			List<Integer> list = formManager.getForms();

			Widget widget = this.extractWidget(reqCtx);
			ApsProperties widgetConfig = widget.getConfig();
			*/
			this.pageContext.setAttribute(this.getVar(), null);// list);
		} catch (Throwable t) {
			_logger.error("Error in doStartTag", t);
			throw new JspException("Error in FormListTag doStartTag", t);
		}
		return super.doStartTag();
	}

/*
	private Widget extractWidget(RequestContext reqCtx) {
		Widget widget = null;
		widget = (Widget) reqCtx.getExtraParam((SystemConstants.EXTRAPAR_CURRENT_WIDGET));
		return widget;
	}

	protected String extractWidgetParameter(String parameterName, ApsProperties widgetConfig, RequestContext reqCtx) {
		return (String) widgetConfig.get(parameterName);
	}
*/
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
