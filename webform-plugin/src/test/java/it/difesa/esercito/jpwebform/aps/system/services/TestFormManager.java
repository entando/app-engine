/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import static it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager.BEAN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.agiletec.aps.BaseTestCase;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.model.FormData;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
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
		Form form = _formManager.getForm(2677);
		assertNotNull(form);
		assertEquals(2677L, form.getId());
		assertEquals("Oettam", form.getName());
		testFormData(form.getData());
	}

	private void testFormData(FormData data) {
		assertEquals("setEtichetta1", data.etichetta1);
	}



//	@Test
//	public void testGetForms() throws Exception {
//
//	}

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

	@Test
	public void testAddForm() throws Exception {
		try {
			Form form = createFormForTesting(null, "entando");
			_formManager.addForm(form);
			assertNotNull(form.getId());
			Form verify = _formManager.getForm(form.getId());
			assertNotNull(verify.getId());
			// TODO completare
			assertEquals("entando", verify.getName());
			testFormData(form.getData());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {

		}
	}

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



	public Form createFormForTesting(LocalDateTime dateTime, String name) {
		if (dateTime == null) {
			dateTime = LocalDateTime.now();
		}
		if (StringUtils.isBlank(name)){
			name = "userme";
		}
		Form form = new Form();
		form.setId(null);
		form.setSubmitted(Date.from(dateTime.atZone(ZONE_ITALY).toInstant()));
		form.setName(name);
		FormData data = generateFormDataForTesting();
		form.setData(data);
		return form;
	}


	private FormData generateFormDataForTesting() {
		FormData fd = new FormData();

		fd.setEtichetta1("setEtichetta1");
		fd.setEtichetta2("setEtichetta2");
		fd.setEtichetta3("setEtichetta3");
		fd.setEtichetta4("setEtichetta4");
		fd.setEtichetta5("setEtichetta5");

		fd.setTesto1("setTesto1");
		fd.setTesto2("setTesto2");
		fd.setTesto3("setTesto3");
		fd.setTesto4("setTesto4");
		fd.setTesto5("setTesto5");

		fd.setEtichettaSel1("setEtichettaSel1");
		fd.setEtichettaSel2("setEtichettaSel2");
		fd.setEtichettaSel2("setEtichettaSel3");
		fd.setEtichettaSel4("setEtichettaSel4");
		fd.setEtichettaSel5("setEtichettaSel5");

		fd.setValore1("setValore1");
		fd.setValore2("setValore2");
		fd.setValore3("setValore3");
		fd.setValore4("setValore4");
		fd.setValore5("setValore5");


		return fd;
	}

	private IFormManager _formManager;
}
