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
package com.agiletec.plugins.jpversioning.apsadmin.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import java.util.List;

import javax.sql.DataSource;

import com.agiletec.plugins.jpversioning.util.JpversioningTestHelper;

import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.apsadmin.content.ContentActionConstants;
import com.agiletec.plugins.jacms.apsadmin.content.AbstractContentAction;
import com.agiletec.plugins.jpversioning.aps.system.JpversioningSystemConstants;
import com.agiletec.plugins.jpversioning.aps.system.services.versioning.ContentVersion;
import com.agiletec.plugins.jpversioning.aps.system.services.versioning.IVersioningManager;
import org.apache.struts2.action.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author G.Cocco
 */
public class TestVersionAction extends ApsAdminBaseTestCase {
	
    @Test
	void testHistory() throws Throwable {
		String result = this.executeHistory("admin", "ART1");
		assertEquals(Action.SUCCESS, result);
		List<Long> versions = ((VersionAction) this.getAction()).getContentVersions();
		assertEquals(3, versions.size());
		ContentVersion version = ((VersionAction) this.getAction()).getContentVersion(1);
		assertEquals("Articolo", version.getDescr());
		
		result = this.executeHistory("admin", "ART2");
		assertEquals(Action.SUCCESS, result);
		versions = ((VersionAction) this.getAction()).getContentVersions();
		assertNull(versions);
	}
	
	@Test
	void testTrash() throws Throwable {
		String result = this.executeTrash("admin", 3);
		assertEquals(Action.SUCCESS, result);
	}
	
	@Test
	void testDelete() throws Throwable {
		String result = this.executeDelete("admin", 300);
		assertEquals(Action.SUCCESS, result);
	}
	
	@Test
	void testPreview() throws Throwable {
		String result = this.executePreview("admin", 3);
		assertEquals(Action.SUCCESS, result);
	}
	
	@Test
	void testEntryRecover() throws Throwable {
		String result = this.executeEntryRecover("admin", 3);
		assertEquals(Action.SUCCESS, result);
	}
    
    @Test
	void testRecoverAction() throws Throwable {
        String contentId = null;
        try {
            Content contentToVersion = this.contentManager.loadContent("ART187", false);
            contentToVersion.setId(null);
            for (int i = 0; i < 15; i++) {
                contentToVersion.setDescription("Version " + i);
                this.contentManager.saveContent(contentToVersion);
                contentId = contentToVersion.getId();
                if (i % 4 == 0) {
                    this.contentManager.insertOnLineContent(contentToVersion);
                }
            }
            String lastVersionDescription = contentToVersion.getDescription();
            List<Long> versions = this.versioningManager.getVersions(contentId);
            int versionSize = versions.size();
            assertTrue(versionSize > 13);
            ContentVersion versionToRestore = this.versioningManager.getVersion(versions.get(7));
            String versionToRestoreDescr = versionToRestore.getDescr();
            assertEquals("3.4", versionToRestore.getVersion());
            Content currentWorkContent = this.contentManager.loadContent(contentId, false);
            assertEquals("5.2", currentWorkContent.getVersion());
            assertEquals("5.0", this.contentManager.loadContent(contentId, true).getVersion());
            String result = this.executeRecover("admin", versions.get(7));
            assertEquals(Action.SUCCESS, result);
            String marker = AbstractContentAction.buildContentOnSessionMarker(contentId, "ART", ApsAdminSystemConstants.EDIT);
            Content contentOnSession = (Content) this.getRequest().getSession().getAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + marker);
            assertNotNull(contentOnSession);
            assertEquals("5.3", contentOnSession.getVersion());
            versions = this.versioningManager.getVersions(contentId);
            assertEquals(versionSize+1, versions.size());
            ContentVersion lastVersion = this.versioningManager.getVersion(versions.get(0));
            assertEquals("5.2", lastVersion.getVersion());
            assertEquals(lastVersionDescription, lastVersion.getDescr());
            Content currentContent = this.contentManager.loadContent(contentId, false);
            assertEquals("5.3", currentContent.getVersion());
            assertEquals(versionToRestoreDescr, currentContent.getDescription());
        } catch (Exception e) {
            throw e;
        } finally {
            Content lastContent = this.contentManager.loadContent(contentId, false);
            this.contentManager.removeOnLineContent(lastContent);
            this.contentManager.deleteContent(contentId);
            this.helper.cleanContentVersions();
        }
    }
	
	public String executeHistory(String username, String contentId) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/jpversioning/Content/Versioning", "history");
		this.addParameter("contentId", contentId);
		String result = this.executeAction();
		return result;
	}
	
	public String executeTrash(String username, long versionId) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/jpversioning/Content/Versioning", "trash");
		this.addParameter("versionId", String.valueOf(versionId));
		String result = this.executeAction();
		return result;
	}
	
	public String executeDelete(String username, long versionId) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/jpversioning/Content/Versioning", "delete");
		this.addParameter("versionId", String.valueOf(versionId));
		String result = this.executeAction();
		return result;
	}
	
	public String executePreview(String username, long versionId) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/jpversioning/Content/Versioning", "preview");
		this.addParameter("versionId", String.valueOf(versionId));
		String result = this.executeAction();
		return result;
	}
	
	public String executeEntryRecover(String username, long versionId) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/jpversioning/Content/Versioning", "entryRecover");
		this.addParameter("versionId", String.valueOf(versionId));
		String result = this.executeAction();
		return result;
	}
	
	public String executeRecover(String username, long versionId) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/jpversioning/Content/Versioning", "recover");
		this.addParameter("versionId", String.valueOf(versionId));
		String result = this.executeAction();
		return result;
	}
	
    @BeforeEach
	protected void init() throws Exception {
		this.contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
        this.versioningManager = (IVersioningManager) this.getService(JpversioningSystemConstants.VERSIONING_MANAGER);
		DataSource dataSource = (DataSource) this.getApplicationContext().getBean("portDataSource");
		this.helper = new JpversioningTestHelper(dataSource, this.getApplicationContext());
		this.helper.initContentVersions();
	}
    
    @AfterEach
	protected void dispose() throws Exception {
		this.helper.cleanContentVersions();
	}
    
	private JpversioningTestHelper helper;
	
	private IContentManager contentManager;
    private IVersioningManager versioningManager;
	
}