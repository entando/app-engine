/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import com.agiletec.aps.BaseTestCase;
import it.difesa.esercito.jpwebform.aps.ApsPluginBaseTestCase;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.FormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager.BEAN_ID;
import static org.junit.Assert.*;

public class TestFormManager extends BaseTestCase {//ApsPluginBaseTestCase

	public static final ZoneId ZONE_ITALY = ZoneId.of("Europe/Rome");

/*	@BeforeAll
	public static void setUp() throws Exception {
		super.setUp();
		//this.init();
	}*/

	public void testGetForm() throws Exception {
		final String fileName = createFileForTesting(null);

		try {
			Form form = _formManager.getForm(fileName);
			assertNotNull(form);
			assertEquals(fileName, form.getId());
		} finally {
			if (StringUtils.isNotBlank(fileName)) {
				_formManager.deleteForm(fileName);
			}
		}
	}

	public void testGetForms() throws Exception {
		String fileName = createFileForTesting(null);
		try {
			List<Form> forms = this._formManager.getForms();
			assertNotNull(forms);
			assertFalse(forms.isEmpty());
		} finally {
			if (StringUtils.isNotBlank(fileName)) {
				_formManager.deleteForm(fileName);
			}
		}
	}

	public void testDeleteExpiredFiles() throws Exception {
		String fileName = null;

		try {
			LocalDateTime expiredFile = LocalDateTime.now();
			expiredFile = expiredFile.minusHours(12);
			fileName = createFileForTesting(expiredFile);
			List<Form> forms = _formManager.getForms();
			assertNotNull(forms);
			assertTrue(forms.isEmpty());
		} finally {
			if (StringUtils.isNotBlank(fileName)) {
				_formManager.deleteForm(fileName);
			}
		}
	}


	public void testAddForm() throws Exception {
		String fileName = null;
		try {
			fileName = createFileForTesting(null);
			assertNotNull(fileName);
		} finally {
			if (StringUtils.isNotBlank(fileName)) {
				_formManager.deleteForm(fileName);
			}
		}
	}

	public void testDeleteForm() throws Exception {
		final String fileName = createFileForTesting(null);
		assertNotNull(fileName);
		Form form = _formManager.getForm(fileName);
		assertNotNull(form);
		_formManager.deleteForm(fileName);
		try {
			form = _formManager.getForm(fileName); // this throws an exception!
			assertNull(form); // this shouldn't be reached
		} catch (Exception e) {
			assertEquals("com.agiletec.aps.system.exception.ApsSystemException", e.getClass().getCanonicalName());
			assertTrue(e.getMessage().contains("Error loading form with id"));
		}
	}

	public static String createFileForTesting(LocalDateTime dateTime) throws Exception {
		if (dateTime == null) {
			dateTime = LocalDateTime.now();
		}
		Form form = TestMapper.getFormForTest();
		form.setSubmitted(Date.from(dateTime.atZone(ZONE_ITALY).toInstant()));
		final String json;
		final long epochMillis = form.getSubmitted().getTime();
		final String fileName = System.getProperty("java.io.tmpdir") +
				File.separator + "sme-form" + File.separator +
				String.valueOf(epochMillis) +
				"-" + FormManager.generateRandomHash(8) +
				".sme";
		final File file = new File(fileName);

		form.setId(file.getName());
		json = form.toJson();
		FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
		return file.getName();
	}

	private void init() {
		this._formManager = (IFormManager) this.getService(BEAN_ID);
	}


	public static void setUp(){

	}
	private IFormManager _formManager;
}
