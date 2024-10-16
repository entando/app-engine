/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.apsadmin.form;

import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.apsadmin.system.BaseAction;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.IFormManager;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import org.entando.entando.plugins.jpwebform.aps.system.services.mail.IMailManager;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormAction extends BaseAction {

	private static final Logger _logger =  LoggerFactory.getLogger(FormAction.class);


	public String getName() {
		return _name;
	}
	public void setName(String name) {
		this._name = name;
	}

	public Date getSubmitted() {
		return _submitted;
	}
	public void setSubmitted(Date submitted) {
		this._submitted = submitted;
	}

	public FormData getData() {
		return _data;
	}
	public void setData(FormData data) {
		this._data = data;
	}

	protected IFormManager getFormManager() {
		return _formManager;
	}
	public void setFormManager(IFormManager formManager) {
		this._formManager = formManager;
	}

	public IMailManager getMailManager() {
		return _mailManager;
	}
	public void setMailManager(IMailManager mailManager) {
		this._mailManager = mailManager;
	}

	public IPageManager getPageManager() {
		return _pageManager;
	}
	public void setPageManager(IPageManager pageManager) {
		this._pageManager = pageManager;
	}

	private String _name;
	private Date _submitted;
	private FormData _data;
	
	private IFormManager _formManager;
	private IMailManager _mailManager;
	private IPageManager _pageManager;
	
}
