/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.jpwebform.aps.system.services;

import com.agiletec.aps.BaseTestCase;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.model.FormData;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.ZoneId;
import java.util.Date;

import static it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager.BEAN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestFormManager extends BaseTestCase {

	public static final ZoneId ZONE_ITALY = ZoneId.of("Europe/Rome");
	private static final Date TODAY = new Date();


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
		assertEquals(new LocalDate(2024,9,5).toDate(), form.getSubmitted());

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

			Form form = getFormForTest();
			_formManager.addForm(form);

			assertNotNull(form.getId());

			Form verify = _formManager.getForm(form.getId());

			assertNotNull(verify.getId());
			assertEquals(2678L, verify.getId()); //(INCREMENTALE)verifica il Max degli Id ed incrementa di uno
			assertEquals("Plinio", verify.getName());
			testFormData(verify.getData());
			assertEquals(verify.getSubmitted(), new LocalDate().now().toDate());

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



/*	public Form createFormForTesting(LocalDateTime dateTime, String name) {
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
	}*/


	/*private FormData generateFormDataForTesting() {
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
	}*/

	private void testFormData(FormData data) {

		assertNotNull(data);

		assertEquals("setValore1", data.valore1);
		assertEquals("setValore2", data.valore2);
		assertEquals("setValore3", data.valore3);
		assertEquals("setValore4", data.valore4);
		assertEquals("setValore5", data.valore5);

		assertEquals("setTesto1", data.testo1);
		assertEquals("setTesto2", data.testo2);
		assertEquals("setTesto3", data.testo3);
		assertEquals("setTesto4", data.testo4);
		assertEquals("setTesto5", data.testo5);

		assertEquals("setEtichettaSel1", data.etichettaSel1);
		assertEquals("setEtichettaSel3", data.etichettaSel2);
		assertEquals(null, data.etichettaSel3);
		assertEquals("setEtichettaSel4", data.etichettaSel4);
		assertEquals("setEtichettaSel5", data.etichettaSel5);

		assertEquals("setEtichetta1", data.etichetta1);
		assertEquals("setEtichetta2", data.etichetta2);
		assertEquals("setEtichetta3", data.etichetta3);
		assertEquals("setEtichetta4", data.etichetta4);
		assertEquals("setEtichetta5", data.etichetta5);

	}

	public static FormData getFormDataForTest() {
		FormData fd = new FormData();

		fd.setValore1("setValore1");
		fd.setValore2("setValore2");
		fd.setValore3("setValore3");
		fd.setValore4("setValore4");
		fd.setValore5("setValore5");

		fd.setTesto1("setTesto1");
		fd.setTesto2("setTesto2");
		fd.setTesto3("setTesto3");
		fd.setTesto4("setTesto4");
		fd.setTesto5("setTesto5");

		fd.setEtichettaSel1("setEtichettaSel1");
		fd.setEtichettaSel2("setEtichettaSel3");
		fd.setEtichettaSel3(null);
		fd.setEtichettaSel4("setEtichettaSel4");
		fd.setEtichettaSel5("setEtichettaSel5");

		fd.setEtichetta1("setEtichetta1");
		fd.setEtichetta2("setEtichetta2");
		fd.setEtichetta3("setEtichetta3");
		fd.setEtichetta4("setEtichetta4");
		fd.setEtichetta5("setEtichetta5");

		return fd;
	}

	public static Form getFormForTest() {
		Form form = new Form();

		form.setId(2677L);
		form.setName("Plinio");
		form.setSubmitted(TODAY);
		form.setData(getFormDataForTest());

		return form;
	}

	private IFormManager _formManager;
}
