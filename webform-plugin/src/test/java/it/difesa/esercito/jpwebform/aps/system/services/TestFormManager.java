/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import static it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager.BEAN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.DtoHelper;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.FormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.directive.contrib.For;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestFormManager extends BaseTestCase {

	public static final ZoneId ZONE_ITALY = ZoneId.of("Europe/Rome");


	@BeforeEach
	public void init() {
		this._formManager = (IFormManager) this.getService(BEAN_ID);
		assertNotNull(_formManager);
		DataSource dataSource = (DataSource) this.getApplicationContext().getBean("portDataSource");
	}

	@Test
	public void testGetForm() throws Exception {
		final String fileName = createFileForTesting(null);
		System.out.println("\t\t\t\t\tNOME GENERATO FILENAME=====> "+fileName+"\n\n\n\n\n\n\n\n\n");

		Form form = _formManager.getForm(fileName);

		System.out.println("\t\t\t\t\tFORM=====> "+form.getId()+"\n\n\n\n\n\n\n\n\n");


/*
		try {
			Form form = _formManager.getForm(fileName);
			assertNotNull(form);
			assertEquals(fileName, form.getId());
		} finally {
			if (StringUtils.isNotBlank(fileName)) {
				_formManager.deleteForm(fileName);
			}
		}*/
	}

	@Test
	public void testGetForms() throws Exception {
		String fileName = createFileForTesting(null);

		System.out.println("\t\t\t\t\tFILENAME=====> "+fileName+"\n\n\n\n\n\n\n\n\n");

		List<Form> forms = this._formManager.getForms();
		System.out.println("\t\t\t\t\tLIST FORMS=====> "+ forms +"\n\n\n\n\n\n\n\n\n");
/*		try {
			List<Form> forms = this._formManager.getForms();
			assertNotNull(forms);
			assertFalse(forms.isEmpty());
		} finally {
			if (StringUtils.isNotBlank(fileName)) {
				_formManager.deleteForm(fileName);
			}
		}*/
	}

/*	@Test
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
	}*/

/*	@Test
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
	}*/

/*	@Test
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
	}*/



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

		form.setId(2677L);
		json = form.toJson();
		FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
		return file.getName();
	}


	private IFormManager _formManager;
}
