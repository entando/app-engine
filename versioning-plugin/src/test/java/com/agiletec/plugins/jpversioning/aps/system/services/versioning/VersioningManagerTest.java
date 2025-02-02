/*
 * Copyright 2024-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jpversioning.aps.system.services.versioning;

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.plugins.jpversioning.aps.system.JpversioningSystemConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersioningManagerTest {
    
    @Mock
    private ConfigInterface configManager;
    
    @InjectMocks
    private VersioningManager versioningManager;
    
    @Test
    void checkRightConfiguration() throws Exception {
        this.checkRightConfiguration(null, true);
        this.checkRightConfiguration("false", false);
        this.checkRightConfiguration("true", true);
        this.checkRightConfiguration("True", true);
        this.checkRightConfiguration("TRUE", true);
        this.checkRightConfiguration("False", false);
        this.checkRightConfiguration("FALSE", false);
    }
    
    private void checkRightConfiguration(String deleteMidVersion, boolean expected) throws Exception {
        String paramName = JpversioningSystemConstants.CONFIG_PARAM_DELETE_MID_VERSIONS;
        Mockito.when(configManager.getParam(paramName)).thenReturn(deleteMidVersion);
        versioningManager.init();
        Assertions.assertEquals(expected, versioningManager.isDeleteMidVersions());
    }
    
}
