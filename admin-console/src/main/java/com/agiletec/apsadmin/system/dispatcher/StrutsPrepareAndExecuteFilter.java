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
package com.agiletec.apsadmin.system.dispatcher;

import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.InitOperations;
import org.apache.struts2.dispatcher.PrepareOperations;

/**
 * Extension of the Struts2 main filter.
 * Handles both the preparation and execution phases of the Struts dispatching process.
 * @author E.Santoboni
 */
public class StrutsPrepareAndExecuteFilter extends org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter {

	@Override
	protected InitOperations createInitOperations() {
		return new com.agiletec.apsadmin.system.dispatcher.InitOperations();
	}

	@Override
	protected PrepareOperations createPrepareOperations(Dispatcher dispatcher) {
		return new com.agiletec.apsadmin.system.dispatcher.PrepareOperations(dispatcher);
	}

}