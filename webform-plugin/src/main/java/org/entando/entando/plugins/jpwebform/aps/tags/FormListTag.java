/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.IFormManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
//		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
//
//		Widget widget = extractWidget(reqCtx);
//		ApsProperties prop = widget.getConfig();

		try {
			List<FieldSearchFilter> filterList = new ArrayList<>();
			if (StringUtils.isNotBlank(getId())) {
				final Long id = Long.parseLong(getId());
				final FieldSearchFilter idFilter = new FieldSearchFilter("id", id, false);
				filterList.add(idFilter);
			}
			if (StringUtils.isNotBlank(getName())) {
				final FieldSearchFilter nameFilter = new FieldSearchFilter("name", getName(), true);
				filterList.add(nameFilter);
			}
			if (StringUtils.isNotBlank(getFrom()) && StringUtils.isNotBlank(getTo())) {
				final Date fromDate = parseDate(getFrom(), true);
				final Date toDate = parseDate(getTo(), false);

				FieldSearchFilter dateFilter = new FieldSearchFilter("submitted", fromDate, toDate);
				filterList.add(dateFilter);
			}
			if (StringUtils.isNotBlank(getDelivered())) {
				final Boolean delivered = Boolean.parseBoolean(getDelivered());
				FieldSearchFilter deliveredFilter = new FieldSearchFilter("delivered", delivered, false);
				filterList.add(deliveredFilter);
			}
			if (StringUtils.isNotBlank(getSeriale())) {
				FieldSearchFilter serialeFilter = new FieldSearchFilter("seriale", getSeriale(), true);
				filterList.add(serialeFilter);
			}
			FieldSearchFilter[] filterArray = filterList.toArray(new FieldSearchFilter[filterList.size()]);
			List<Long> list = formManager.search(filterArray);
			this.pageContext.setAttribute(this.getVar(), list);
		} catch (Throwable t) {
			_logger.error("Error in doStartTag", t);
			throw new JspException("Error in FormListTag doStartTag", t);
		}
		return super.doStartTag();
	}

	private Date parseDate(String dateString, boolean isBeginning) throws ParseException {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		final Date date = dateFormat.parse(dateString);
		final Calendar calendar = Calendar.getInstance();

		calendar.setTime(date);
		if (isBeginning) {
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
		} else {
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 0);
		}
		return calendar.getTime();
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

	public String getFrom() {
		return _from;
	}

	public void setFrom(String _from) {
		this._from = _from;
	}

	public String getTo() {
		return _to;
	}

	public void setTo(String _to) {
		this._to = _to;
	}

	@Override
	public String getId() {
		return _id;
	}

	@Override
	public void setId(String id) {
		this._id = id;
	}

	public String getName() {
		return _name;
	}

	public void setName(String _name) {
		this._name = _name;
	}

	public String getSeriale() {
		return _seriale;
	}

	public void setSeriale(String _seriale) {
		this._seriale = _seriale;
	}

	public String getDelivered() {
		return _delivered;
	}

	public void setDelivered(String _delivered) {
		this._delivered = _delivered;
	}

	private String _var;

	// search param
	private String _from;
	private String _to;
	private String _id;
	private String _name;
	private String _seriale;
	private String _delivered;

}
