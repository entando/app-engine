/*
 * Copyright 2017-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.model.ContentThreadConfig;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.model.ContentTypeElem;
import org.junit.jupiter.api.Test;

/**
 * The "suspend" flag drives the action performed at the end date: "true" unpublishes the
 * content, "false" moves it to the online archive. Both values have to survive the write
 * and read of the cthread_config item, otherwise the content type is silently skipped.
 */
class ContentThreadConfigDOMTest {

    private static final String CONFIG_RESOURCE = "liquibase/jpcontentscheduler/port/clob/test/cthread_config.xml";
    private static final String TYPE_CODE = "NOL";

    @Test
    void suspendTrueShouldSurviveTheConfigRoundTrip() throws Exception {
        ContentThreadConfigDOM dom = new ContentThreadConfigDOM();
        ContentThreadConfig config = dom.extractConfig(this.readSeedConfig());
        assertEquals("true", this.getType(config, TYPE_CODE).getSuspend());

        ContentThreadConfig reloaded = dom.extractConfig(dom.createConfigXml(config));

        assertEquals("true", this.getType(reloaded, TYPE_CODE).getSuspend());
    }

    @Test
    void suspendFalseShouldSurviveTheConfigRoundTrip() throws Exception {
        ContentThreadConfigDOM dom = new ContentThreadConfigDOM();
        ContentThreadConfig config = dom.extractConfig(this.readSeedConfig());
        this.getType(config, TYPE_CODE).setSuspend("false");

        ContentThreadConfig reloaded = dom.extractConfig(dom.createConfigXml(config));

        assertEquals("false", this.getType(reloaded, TYPE_CODE).getSuspend());
    }

    @Test
    void aNullSuspendShouldBeReadBackAsBlank() throws Exception {
        ContentThreadConfigDOM dom = new ContentThreadConfigDOM();
        ContentThreadConfig config = dom.extractConfig(this.readSeedConfig());
        this.getType(config, TYPE_CODE).setSuspend(null);

        ContentThreadConfig reloaded = dom.extractConfig(dom.createConfigXml(config));

        // safeSetAttr() drops null values, so the attribute never reaches the xml and the
        // content type ends up doing nothing at the end date: this is why the admin form
        // has to submit an explicit "false" for an unchecked box.
        assertEquals("", this.getType(reloaded, TYPE_CODE).getSuspend());
    }

    private String readSeedConfig() throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            assertNotNull(is, "Missing test resource " + CONFIG_RESOURCE);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private ContentTypeElem getType(ContentThreadConfig config, String typeCode) {
        return config.getTypesList().stream()
                .filter(elem -> typeCode.equals(elem.getContentType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Content type not configured: " + typeCode));
    }

}