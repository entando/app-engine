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
package org.entando.entando.plugins.jpsolr.apsadmin.page.specialwidget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SmallContentType;
import com.opensymphony.xwork2.Action;
import java.util.List;
import org.entando.entando.plugins.jpsolr.apsadmin.ApsAdminPluginBaseTestCase;
import org.entando.entando.plugins.jpsolr.apsadmin.portal.specialwidget.FacetNavResultWidgetAction;
import org.junit.jupiter.api.Test;

class TestFacetNavResultWidgetAction extends ApsAdminPluginBaseTestCase {

    @Test
    void testInitConfig_1() throws Exception {
        String result = this.executeConfigFacetNavResult("admin", "homepage", "1", "jpsolr_facetResults");
        assertEquals(Action.SUCCESS, result);
        FacetNavResultWidgetAction action = (FacetNavResultWidgetAction) this.getAction();
        Widget widget = action.getWidget();
        assertNotNull(widget);
        assertEquals(0, widget.getConfig().size());
        List<SmallContentType> contentTypes = action.getContentTypes();
        assertNotNull(contentTypes);
        assertEquals(4, contentTypes.size());
    }

    private String executeConfigFacetNavResult(String username, String pageCode, String frame, String widgetTypeCode)
            throws Exception {
        this.setUserOnSession(username);
        this.initAction("/do/Page/SpecialWidget", "solrFacetNavResultConfig");
        this.addParameter("pageCode", pageCode);
        this.addParameter("frame", frame);
        if (null != widgetTypeCode && widgetTypeCode.trim().length() > 0) {
            this.addParameter("widgetTypeCode", widgetTypeCode);
        }
        return this.executeAction();
    }

}