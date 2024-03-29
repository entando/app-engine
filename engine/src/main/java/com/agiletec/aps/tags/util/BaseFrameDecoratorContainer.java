/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.agiletec.aps.tags.util;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.page.Widget;

/**
 * @author E.Santoboni
 */
public class BaseFrameDecoratorContainer implements IFrameDecoratorContainer {
	
	@Override
	public boolean needsDecoration(Widget widget, RequestContext reqCtx) {
		return true;
	}
	
	@Override
	public boolean isWidgetDecorator() {
		return false;
	}

	@Override
	public String getHeaderJspPath() {
		return _headerJspPath;
	}
	public void setHeaderJspPath(String headerJspPath) {
		this._headerJspPath = headerJspPath;
	}
	
	@Override
	public String getHeaderFragmentCode() {
		return _headerFragmentCode;
	}
	public void setHeaderFragmentCode(String headerFragmentCode) {
		this._headerFragmentCode = headerFragmentCode;
	}

	@Deprecated
	public void setFooterPath(String footerPath) {
		this.setFooterJspPath(footerPath);
	}

	@Override
	public String getFooterJspPath() {
		return _footerJspPath;
	}
	public void setFooterJspPath(String footerJspPath) {
		this._footerJspPath = footerJspPath;
	}
	
	@Override
	public String getFooterFragmentCode() {
		return _footerFragmentCode;
	}
	public void setFooterFragmentCode(String footerFragmentCode) {
		this._footerFragmentCode = footerFragmentCode;
	}
	
	@Override
	public int getOrder() {
		return _order;
	}
	public void setOrder(int order) {
		this._order = order;
	}
	
	private String _headerJspPath;
	private String _headerFragmentCode;
	private String _footerJspPath;
	private String _footerFragmentCode;
	private int _order;
	
}
