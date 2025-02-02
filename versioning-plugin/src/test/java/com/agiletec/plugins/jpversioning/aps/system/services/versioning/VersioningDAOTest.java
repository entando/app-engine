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
package com.agiletec.plugins.jpversioning.aps.system.services.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.agiletec.aps.BaseTestCase;
import java.util.List;

import javax.sql.DataSource;

import com.agiletec.aps.util.DateConverter;

import com.agiletec.plugins.jpversioning.util.JpversioningTestHelper;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author G.Cocco
 */
class VersioningDAOTest extends BaseTestCase {
	
    @Test
	void testGetVersions() throws Throwable {
		List<Long> versions = this._versioningDAO.getVersions("CNG12");
		assertNull(versions);
		
		versions = this._versioningDAO.getVersions("ART1");
		this.checkVersionIds(new long[] { 1, 2, 3 }, versions);
	}
	
	@Test
	void testGetLastVersions() throws Throwable {
		List<Long> versions = this._versioningDAO.getLastVersions("CNG", null);
		assertTrue(versions.isEmpty());
		
		versions = this._versioningDAO.getLastVersions("ART", null);
		this.checkVersionIds(new long[] { 3 }, versions);
	}
	
	@Test
	void testGetVersion() {
		ContentVersion contentVersion = this._versioningDAO.getVersion(10000);
		assertNull(contentVersion);	
		long id = 1;
		contentVersion = this._versioningDAO.getVersion(id);
		assertEquals(id, contentVersion.getId());
		assertEquals("ART1", contentVersion.getContentId());
		assertEquals("ART", contentVersion.getContentType());
		assertEquals("Articolo", contentVersion.getDescr());
		assertEquals(Content.STATUS_DRAFT, contentVersion.getStatus());
		assertNotNull(contentVersion.getXml());
		assertEquals("13/02/2005", DateConverter.getFormattedDate(contentVersion.getVersionDate(), "dd/MM/yyyy"));
		assertEquals("0.0", contentVersion.getVersion());
		assertEquals(0, contentVersion.getOnlineVersion());
		assertTrue(contentVersion.isApproved());
		assertEquals("admin", contentVersion.getUsername());
	}
	
	@Test
	void testGetLastVersion() {
		ContentVersion contentVersion = this._versioningDAO.getLastVersion("CNG12");
		assertNull(contentVersion);
		contentVersion = this._versioningDAO.getLastVersion("ART1");
		assertEquals(3, contentVersion.getId());
		assertEquals("ART1", contentVersion.getContentId());
		assertEquals("ART", contentVersion.getContentType());
		assertEquals("Articolo 3", contentVersion.getDescr());
		assertEquals(Content.STATUS_READY, contentVersion.getStatus());
		assertNotNull(contentVersion.getXml());
		assertEquals("15/02/2005", DateConverter.getFormattedDate(contentVersion.getVersionDate(), "dd/MM/yyyy"));
		assertEquals("1.0", contentVersion.getVersion());
		assertEquals(1, contentVersion.getOnlineVersion());
		assertTrue(contentVersion.isApproved());
		assertEquals("mainEditor", contentVersion.getUsername());
	}
	
	@Test
	void testAddGetDeleteVersion() {
		ContentVersion versionRecord = this._versioningDAO.getVersion(1);
		assertNotNull(versionRecord);
		versionRecord.setVersion("1.1");
		versionRecord.setOnlineVersion(1);
		this._versioningDAO.addContentVersion(versionRecord);
		ContentVersion newVersionRecord = this._versioningDAO.getVersion(versionRecord.getId());
		assertNotNull(newVersionRecord);
		this._versioningDAO.deleteVersion(versionRecord.getId());
		newVersionRecord = this._versioningDAO.getVersion(versionRecord.getId());
		assertNull(newVersionRecord);		
	}
	
	@Test
	void testDeleteWorkVersions() {
		List<Long> versions = this._versioningDAO.getVersions("ART1");
		this.checkVersionIds(new long[] { 1, 2, 3 }, versions);
		
		this._versioningDAO.deleteWorkVersions("ART1", 0);
		versions = this._versioningDAO.getVersions("ART1");
		this.checkVersionIds(new long[] { 1, 3 }, versions);
	}
	
	private void checkVersionIds(long[] expected, List<Long> received) {
		assertEquals(expected.length, received.size());
		for (long current : expected) {
			if (!received.contains(new Long(current))) {
				fail("Expected " + current + " - Not found");
			}
		}
	}
	
    @BeforeEach
	private void init() throws Exception {
		VersioningDAO versioningDAO = new VersioningDAO();
		DataSource dataSource = (DataSource) this.getApplicationContext().getBean("portDataSource");
		versioningDAO.setDataSource(dataSource);
		this._versioningDAO = versioningDAO;
		this._helper = new JpversioningTestHelper(dataSource, this.getApplicationContext());
        this._helper.initContentVersions();
	}
    
    @AfterEach
    private void dispose() throws Exception {
        this._helper.cleanContentVersions();
    }
	
	private IVersioningDAO _versioningDAO;
	private JpversioningTestHelper _helper;
	
}